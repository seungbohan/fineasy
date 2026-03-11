package com.fineasy.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    private static final Logger log = LoggerFactory.getLogger(ShedLockConfig.class);

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createTableIfNotExists(jdbcTemplate);

        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(jdbcTemplate)
                        .usingDbTime()
                        .build()
        );
    }

    private void createTableIfNotExists(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS shedlock (
                        name VARCHAR(64) NOT NULL,
                        lock_until TIMESTAMP NOT NULL,
                        locked_at TIMESTAMP NOT NULL,
                        locked_by VARCHAR(255) NOT NULL,
                        PRIMARY KEY (name)
                    )
                    """);
            log.info("ShedLock table verified/created successfully");
        } catch (Exception e) {
            log.warn("Failed to create shedlock table (may already exist): {}", e.getMessage());
        }
    }
}
