package com.fineasy.service;

import com.fineasy.dto.request.FeedbackRequest;
import com.fineasy.dto.response.FeedbackResponse;
import com.fineasy.entity.FeedbackSubmissionEntity;
import com.fineasy.exception.RateLimitExceededException;
import com.fineasy.repository.FeedbackSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    private static final int MAX_SUBMISSIONS_PER_HOUR = 5;

    private final FeedbackSubmissionRepository feedbackRepository;

    public FeedbackService(FeedbackSubmissionRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request, String ipAddress) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = feedbackRepository.countByIpAddressSince(ipAddress, oneHourAgo);

        if (recentCount >= MAX_SUBMISSIONS_PER_HOUR) {
            throw new RateLimitExceededException(
                    "Too many feedback submissions. Please try again later (max " +
                            MAX_SUBMISSIONS_PER_HOUR + " per hour).");
        }

        FeedbackSubmissionEntity entity = new FeedbackSubmissionEntity(
                request.type(),
                request.title(),
                request.content(),
                request.contactEmail(),
                ipAddress
        );

        FeedbackSubmissionEntity saved = feedbackRepository.save(entity);
        log.info("Feedback submitted: id={}, type={}, ip={}", saved.getId(), saved.getType(), ipAddress);

        return new FeedbackResponse(
                saved.getId(),
                saved.getType().name(),
                saved.getTitle(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        );
    }
}
