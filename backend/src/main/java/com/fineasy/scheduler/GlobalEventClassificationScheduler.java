package com.fineasy.scheduler;

import com.fineasy.entity.GlobalEventEntity;
import com.fineasy.entity.NewsArticleEntity;
import com.fineasy.repository.NewsArticleRepository;
import com.fineasy.service.GlobalEventClassifierService;
import com.fineasy.service.GlobalEventClassifierService.KeywordMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "news.rss.collection-enabled", havingValue = "true")
public class GlobalEventClassificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(GlobalEventClassificationScheduler.class);

    private static final int BATCH_SIZE = 100;

    private final NewsArticleRepository newsArticleRepository;
    private final GlobalEventClassifierService classifierService;

    public GlobalEventClassificationScheduler(NewsArticleRepository newsArticleRepository,
                                               GlobalEventClassifierService classifierService) {
        this.newsArticleRepository = newsArticleRepository;
        this.classifierService = classifierService;
        log.info("Global event classification scheduler initialized.");
    }

    @Scheduled(fixedRate = 3600000, initialDelay = 300000)
    @SchedulerLock(name = "classifyRecentNews", lockAtLeastFor = "PT5M", lockAtMostFor = "PT25M")
    public void classifyRecentNews() {
        log.info("Starting global event classification...");

        try {
            Page<NewsArticleEntity> recentNews = newsArticleRepository
                    .findAllOrderByPublishedAtDesc(PageRequest.of(0, BATCH_SIZE));

            if (recentNews.isEmpty()) {
                log.info("No recent news articles to classify.");
                return;
            }

            List<NewsArticleEntity> articles = recentNews.getContent();
            log.info("Fetched {} recent news articles for classification.", articles.size());

            List<KeywordMatchResult> matches = classifierService.filterByKeywords(articles);

            if (matches.isEmpty()) {
                log.info("No keyword matches found in this cycle.");
                return;
            }

            List<GlobalEventEntity> savedEvents = classifierService.classifyAndSave(matches);
            log.info("Global event classification completed: {} events saved.", savedEvents.size());

        } catch (Exception e) {
            log.error("Global event classification failed", e);
        }
    }
}
