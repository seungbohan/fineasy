package com.fineasy.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fineasy.entity.BokTermEntity;
import com.fineasy.repository.BokTermRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BokTermDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BokTermDataLoader.class);

    private static final String JSON_RESOURCE_PATH = "data/bok-financial-terms.json";

    private final BokTermRepository bokTermRepository;
    private final ObjectMapper objectMapper;

    public BokTermDataLoader(BokTermRepository bokTermRepository, ObjectMapper objectMapper) {
        this.bokTermRepository = bokTermRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bokTermRepository.count() > 0) {
            log.info("BOK terms already loaded ({} entries), skipping.", bokTermRepository.count());
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource(JSON_RESOURCE_PATH);
            if (!resource.exists()) {
                log.warn("BOK terms JSON not found at classpath:{} - skipping.", JSON_RESOURCE_PATH);
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<Map<String, String>> termsData = objectMapper.readValue(
                        is, new TypeReference<>() {});

                LocalDateTime now = LocalDateTime.now();
                List<BokTermEntity> batch = new ArrayList<>();

                for (Map<String, String> data : termsData) {
                    String term = data.get("term");
                    String englishTerm = data.get("englishTerm");
                    String definition = data.get("definition");
                    String category = data.get("category");

                    if (term == null || term.isBlank() || definition == null || definition.isBlank()) {
                        continue;
                    }

                    batch.add(new BokTermEntity(
                            null, term.trim(), englishTerm, definition.trim(), category, now));
                }

                if (!batch.isEmpty()) {
                    bokTermRepository.saveAll(batch);
                }
                log.info("Loaded {} BOK financial terms from JSON.", batch.size());
            }
        } catch (Exception e) {
            log.error("Failed to load BOK terms from JSON: {}", e.getMessage(), e);
        }
    }
}
