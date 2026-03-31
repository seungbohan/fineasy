package com.fineasy.repository;

import com.fineasy.entity.FeedbackSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmissionEntity, Long> {

    @Query("SELECT COUNT(f) FROM FeedbackSubmissionEntity f " +
            "WHERE f.ipAddress = :ipAddress AND f.createdAt >= :since")
    long countByIpAddressSince(@Param("ipAddress") String ipAddress,
                               @Param("since") LocalDateTime since);
}
