package com.fineasy.scheduler;

import com.fineasy.config.NewsRssProperties;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.repository.NewsArticleRepository;
import com.fineasy.service.NewsCollectorService;
import com.fineasy.service.NewsSentimentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "news.rss.collection-enabled", havingValue = "true")
@EnableConfigurationProperties(NewsRssProperties.class)
public class NewsCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectionScheduler.class);

    private static final int RETRY_BATCH_SIZE = 100;

    private final NewsCollectorService newsCollectorService;
    private final NewsSentimentService newsSentimentService;
    private final NewsArticleRepository newsArticleRepository;
    private final List<String> rssUrls;

    public NewsCollectionScheduler(NewsCollectorService newsCollectorService,
                                    NewsSentimentService newsSentimentService,
                                    NewsArticleRepository newsArticleRepository,
                                    NewsRssProperties newsRssProperties) {
        this.newsCollectorService = newsCollectorService;
        this.newsSentimentService = newsSentimentService;
        this.newsArticleRepository = newsArticleRepository;
        this.rssUrls = newsRssProperties.getUrls();
        log.info("News collection scheduler initialized with {} RSS URLs: {}",
                rssUrls.size(), rssUrls);
    }

    @Scheduled(fixedRate = 1800000)
    @SchedulerLock(name = "collectAndAnalyzeNews", lockAtLeastFor = "PT5M", lockAtMostFor = "PT25M")
    public void collectAndAnalyzeNews() {
        log.info("Starting scheduled news collection...");

        List<NewsArticleEntity> allNewArticles = new ArrayList<>();

        for (String rssUrl : rssUrls) {
            if (rssUrl == null || rssUrl.isBlank()) continue;

            try {
                List<NewsArticleEntity> newArticles = newsCollectorService.collectFromRss(rssUrl);
                allNewArticles.addAll(newArticles);
            } catch (Exception e) {
                log.error("Failed to collect news from {}: {}", rssUrl, e.getMessage());
            }
        }

        if (!allNewArticles.isEmpty()) {
            log.info("Collected {} new articles, starting sentiment analysis...", allNewArticles.size());
            try {
                newsSentimentService.analyzeSentiment(allNewArticles);
            } catch (Exception e) {
                log.error("Sentiment analysis failed for collected news", e);
            }
        } else {
            log.info("No new articles collected in this cycle.");
        }

        retryUnanalyzedArticles();

        log.info("Scheduled news collection completed.");
    }

    private void retryUnanalyzedArticles() {
        try {
            List<NewsArticleEntity> unanalyzed = newsArticleRepository
                    .findBySentimentIsNull(PageRequest.of(0, RETRY_BATCH_SIZE));

            if (!unanalyzed.isEmpty()) {
                log.info("Retrying sentiment analysis for {} unanalyzed articles", unanalyzed.size());
                newsSentimentService.analyzeSentiment(unanalyzed);
            }
        } catch (Exception e) {
            log.error("Retry sentiment analysis failed", e);
        }
    }
}
