package com.fineasy.service;

import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.repository.NewsArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class NewsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectorService.class);

    private static final DateTimeFormatter RSS_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private final NewsArticleRepository newsArticleRepository;
    private final WebClient webClient;

    public NewsCollectorService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Transactional
    public List<NewsArticleEntity> collectFromRss(String rssUrl) {
        log.info("Collecting news from RSS: {}", rssUrl);
        List<NewsArticleEntity> savedArticles = new ArrayList<>();

        try {
            String xmlContent = fetchRssContent(rssUrl);
            List<RssItem> items = parseRssXml(xmlContent);

            log.info("Parsed {} items from RSS feed", items.size());

            java.util.Set<String> existingUrls = newsArticleRepository.findExistingUrls(
                    items.stream().map(i -> truncate(i.link(), 1000))
                            .filter(java.util.Objects::nonNull).toList());
            java.util.Set<String> existingTitles = newsArticleRepository.findExistingTitles(
                    items.stream().map(i -> truncate(i.title(), 500))
                            .filter(java.util.Objects::nonNull).toList());

            for (RssItem item : items) {
                try {
                    String url = truncate(item.link(), 1000);
                    String title = truncate(item.title(), 500);

                    if (url != null && existingUrls.contains(url)) {
                        log.debug("Duplicate URL, skipping: {}", url);
                        continue;
                    }
                    if (title != null && existingTitles.contains(title)) {
                        log.debug("Duplicate title, skipping: {}", title);
                        continue;
                    }

                    NewsArticleEntity article = new NewsArticleEntity(
                            null,
                            title,
                            null,
                            url,
                            truncate(item.source(), 100),
                            item.publishedAt(),
                            null,
                            null,
                            null,
                            null
                    );
                    NewsArticleEntity saved = newsArticleRepository.save(article);
                    savedArticles.add(saved);
                } catch (Exception e) {
                    log.warn("Failed to save news article: {}", item.title(), e);
                }
            }

            log.info("Successfully saved {} new articles from {}", savedArticles.size(), rssUrl);
        } catch (Exception e) {
            log.error("Failed to collect news from RSS: {}", rssUrl, e);
        }

        return savedArticles;
    }

    private String fetchRssContent(String rssUrl) {
        return webClient.get()
                .uri(rssUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block(java.time.Duration.ofSeconds(15));
    }

    private List<RssItem> parseRssXml(String xml) {
        List<RssItem> items = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            log.warn("RSS content is null or empty, skipping parse");
            return items;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element item = (Element) itemNodes.item(i);
                String title = getElementText(item, "title");
                String link = getElementText(item, "link");
                String source = getElementText(item, "source");
                String pubDate = getElementText(item, "pubDate");

                if (title == null || link == null) continue;

                if (source == null || source.isBlank()) {
                    source = extractDomain(link);
                }

                LocalDateTime publishedAt = parsePubDate(pubDate);

                items.add(new RssItem(title.trim(), link.trim(), source, publishedAt));
            }
        } catch (Exception e) {
            log.error("Failed to parse RSS XML", e);
        }

        return items;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private static final List<DateTimeFormatter> FALLBACK_FORMATTERS = List.of(

            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),

            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss XXX", Locale.ENGLISH),

            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss XXX", Locale.ENGLISH),

            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
    );

    private LocalDateTime parsePubDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return LocalDateTime.now();
        }
        String trimmed = pubDate.trim();

        try {
            return ZonedDateTime.parse(trimmed, RSS_DATE_FORMATTER).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        for (DateTimeFormatter formatter : FALLBACK_FORMATTERS) {
            try {
                return ZonedDateTime.parse(trimmed, formatter).toLocalDateTime();
            } catch (DateTimeParseException ignored) {}
        }

        try {
            return ZonedDateTime.parse(trimmed).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}

        log.debug("Failed to parse date '{}', using current time", pubDate);
        return LocalDateTime.now();
    }

    private String extractDomain(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private record RssItem(String title, String link, String source, LocalDateTime publishedAt) {}
}
