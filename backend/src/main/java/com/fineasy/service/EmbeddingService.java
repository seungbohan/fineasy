package com.fineasy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.config.OpenAiConfig;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.entity.NewsEmbeddingEntity;
import com.fineasy.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages news article embeddings using OpenAI text-embedding-3-small.
 * Provides semantic search for RAG-based analysis.
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int EMBEDDING_DIMENSIONS = 1536;
    private static final int BATCH_SIZE = 50;

    private final WebClient webClient;
    private final OpenAiConfig openAiConfig;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final NewsArticleRepository newsArticleRepository;

    public EmbeddingService(@Qualifier("openAiWebClient") WebClient webClient,
                             OpenAiConfig openAiConfig,
                             ObjectMapper objectMapper,
                             JdbcTemplate jdbcTemplate,
                             NewsArticleRepository newsArticleRepository) {
        this.webClient = webClient;
        this.openAiConfig = openAiConfig;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.newsArticleRepository = newsArticleRepository;
    }

    /**
     * Generate embedding for a single text.
     */
    public float[] generateEmbedding(String text) {
        List<float[]> results = generateEmbeddings(List.of(text));
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Generate embeddings for multiple texts in one API call.
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();

        Map<String, Object> requestBody = Map.of(
                "model", EMBEDDING_MODEL,
                "input", texts
        );

        try {
            String responseBody = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(openAiConfig.getTimeout());

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embeddingArray = item.path("embedding");
                float[] embedding = new float[EMBEDDING_DIMENSIONS];
                for (int i = 0; i < EMBEDDING_DIMENSIONS && i < embeddingArray.size(); i++) {
                    embedding[i] = (float) embeddingArray.get(i).asDouble();
                }
                embeddings.add(embedding);
            }
            return embeddings;
        } catch (Exception e) {
            log.error("Failed to generate embeddings: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Store embedding for a news article using native SQL (pgvector).
     */
    public void saveEmbedding(Long newsArticleId, float[] embedding) {
        String vectorStr = toVectorString(embedding);
        jdbcTemplate.update(
                "INSERT INTO news_embeddings (news_article_id, embedding, created_at) " +
                        "VALUES (?, ?::vector, NOW()) " +
                        "ON CONFLICT (news_article_id) DO UPDATE SET embedding = ?::vector",
                newsArticleId, vectorStr, vectorStr);
    }

    /**
     * Semantic search: find news articles most similar to the query.
     * Uses cosine distance with pgvector.
     */
    public List<NewsArticleEntity> searchSimilarNews(String query, int limit) {
        float[] queryEmbedding = generateEmbedding(query);
        if (queryEmbedding == null) return List.of();

        return searchByEmbedding(queryEmbedding, limit, null);
    }

    /**
     * Semantic search within a time range.
     */
    public List<NewsArticleEntity> searchSimilarNews(String query, int limit, LocalDateTime since) {
        float[] queryEmbedding = generateEmbedding(query);
        if (queryEmbedding == null) return List.of();

        return searchByEmbedding(queryEmbedding, limit, since);
    }

    private List<NewsArticleEntity> searchByEmbedding(float[] embedding, int limit, LocalDateTime since) {
        String vectorStr = toVectorString(embedding);

        String sql;
        Object[] params;

        if (since != null) {
            sql = "SELECT ne.news_article_id FROM news_embeddings ne " +
                    "JOIN news_articles na ON na.id = ne.news_article_id " +
                    "WHERE na.published_at >= ? " +
                    "ORDER BY ne.embedding <=> ?::vector " +
                    "LIMIT ?";
            params = new Object[]{java.sql.Timestamp.valueOf(since), vectorStr, limit};
        } else {
            sql = "SELECT ne.news_article_id FROM news_embeddings ne " +
                    "ORDER BY ne.embedding <=> ?::vector " +
                    "LIMIT ?";
            params = new Object[]{vectorStr, limit};
        }

        List<Long> articleIds = jdbcTemplate.queryForList(sql, Long.class, params);

        if (articleIds.isEmpty()) return List.of();

        return newsArticleRepository.findAllById(articleIds);
    }

    /**
     * Periodically embed news articles that don't have embeddings yet.
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 600000) // Every 1 hour, initial delay 10 min
    @SchedulerLock(name = "embedNewArticles", lockAtLeastFor = "PT5M", lockAtMostFor = "PT25M")
    public void embedNewArticles() {
        try {
            // Check if pgvector extension and table exist
            ensurePgvectorSetup();

            List<Long> unembeddedIds = jdbcTemplate.queryForList(
                    "SELECT na.id FROM news_articles na " +
                            "LEFT JOIN news_embeddings ne ON na.id = ne.news_article_id " +
                            "WHERE ne.id IS NULL " +
                            "ORDER BY na.published_at DESC " +
                            "LIMIT ?",
                    Long.class, BATCH_SIZE * 5);

            if (unembeddedIds.isEmpty()) {
                log.debug("No unembedded news articles found.");
                return;
            }

            log.info("Embedding {} unembedded news articles...", unembeddedIds.size());

            List<NewsArticleEntity> articles = newsArticleRepository.findAllById(unembeddedIds);

            // Process in batches
            int success = 0;
            for (int i = 0; i < articles.size(); i += BATCH_SIZE) {
                List<NewsArticleEntity> batch = articles.subList(i, Math.min(i + BATCH_SIZE, articles.size()));

                List<String> texts = batch.stream()
                        .map(a -> buildEmbeddingText(a))
                        .toList();

                List<float[]> embeddings = generateEmbeddings(texts);

                for (int j = 0; j < embeddings.size() && j < batch.size(); j++) {
                    saveEmbedding(batch.get(j).getId(), embeddings.get(j));
                    success++;
                }

                if (i + BATCH_SIZE < articles.size()) {
                    Thread.sleep(200); // Brief pause between batches
                }
            }

            log.info("Embedding completed: {}/{} articles embedded.", success, articles.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to embed articles: {}", e.getMessage());
        }
    }

    private String buildEmbeddingText(NewsArticleEntity article) {
        StringBuilder sb = new StringBuilder();
        sb.append(article.getTitle());
        if (article.getContent() != null && !article.getContent().isBlank()) {
            String content = article.getContent().length() > 500
                    ? article.getContent().substring(0, 500)
                    : article.getContent();
            sb.append(" ").append(content);
        }
        return sb.toString();
    }

    private void ensurePgvectorSetup() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS news_embeddings (" +
                            "id BIGSERIAL PRIMARY KEY, " +
                            "news_article_id BIGINT NOT NULL UNIQUE REFERENCES news_articles(id), " +
                            "embedding vector(1536) NOT NULL, " +
                            "created_at TIMESTAMP NOT NULL DEFAULT NOW())");
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_news_embeddings_vector " +
                            "ON news_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)");
        } catch (Exception e) {
            // IVFFlat index requires data, silently ignore if not enough rows
            log.debug("pgvector setup note: {}", e.getMessage());
        }
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
