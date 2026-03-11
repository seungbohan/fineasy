package com.fineasy.service;

import com.fineasy.dto.response.GlobalEventResponse;
import com.fineasy.entity.EventType;
import com.fineasy.entity.GlobalEventEntity;
import com.fineasy.entity.RiskLevel;
import com.fineasy.repository.GlobalEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GlobalEventService {

    private static final Logger log = LoggerFactory.getLogger(GlobalEventService.class);

    private final GlobalEventRepository globalEventRepository;

    public GlobalEventService(GlobalEventRepository globalEventRepository) {
        this.globalEventRepository = globalEventRepository;
    }

    @Cacheable(value = "global-events", key = "'type:' + #type.name() + ':p' + #page + ':s' + #size",
            unless = "#result == null")
    public GlobalEventResponse getEvents(EventType type, int page, int size) {
        Page<GlobalEventEntity> eventPage = globalEventRepository
                .findByEventTypeOrderByPublishedAtDesc(type, PageRequest.of(page, size));
        return toResponse(eventPage, page, size);
    }

    @Cacheable(value = "global-events", key = "'risk:' + #levels.toString() + ':p' + #page + ':s' + #size",
            unless = "#result == null")
    public GlobalEventResponse getEventsByRiskLevel(List<RiskLevel> levels, int page, int size) {
        Page<GlobalEventEntity> eventPage = globalEventRepository
                .findByRiskLevelInOrderByPublishedAtDesc(levels, PageRequest.of(page, size));
        return toResponse(eventPage, page, size);
    }

    @Cacheable(value = "global-events-alerts", unless = "#result == null")
    public GlobalEventResponse getHighRiskEvents() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<GlobalEventEntity> events = globalEventRepository.findRecent24h(since);
        List<GlobalEventResponse.EventData> eventDataList = events.stream()
                .map(this::toEventData)
                .toList();

        return new GlobalEventResponse(eventDataList, events.size(), 0, events.size());
    }

    public GlobalEventResponse getEvents(String type, String riskLevel, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        if (type != null && !type.isBlank()) {
            EventType eventType = parseEventType(type);
            return getEvents(eventType, safePage, safeSize);
        } else if (riskLevel != null && !riskLevel.isBlank()) {
            List<RiskLevel> levels = parseRiskLevels(riskLevel);
            return getEventsByRiskLevel(levels, safePage, safeSize);
        } else {
            return getAllEvents(safePage, safeSize);
        }
    }

    @Cacheable(value = "global-events", key = "'all:p' + #page + ':s' + #size",
            unless = "#result == null")
    public GlobalEventResponse getAllEvents(int page, int size) {
        Page<GlobalEventEntity> eventPage = globalEventRepository
                .findAllOrderByPublishedAtDesc(PageRequest.of(page, size));
        return toResponse(eventPage, page, size);
    }

    private GlobalEventResponse toResponse(Page<GlobalEventEntity> eventPage, int page, int size) {
        List<GlobalEventResponse.EventData> eventDataList = eventPage.getContent().stream()
                .map(this::toEventData)
                .toList();

        return new GlobalEventResponse(
                eventDataList,
                eventPage.getTotalElements(),
                page,
                size
        );
    }

    private EventType parseEventType(String type) {
        try {
            return EventType.valueOf(type.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid event type: " + type +
                    ". Valid values: GEOPOLITICAL, FISCAL, INDUSTRY, BLACK_SWAN");
        }
    }

    private List<RiskLevel> parseRiskLevels(String riskLevel) {
        try {
            return Arrays.stream(riskLevel.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(RiskLevel::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid risk level: " + riskLevel +
                    ". Valid values: LOW, MEDIUM, HIGH, CRITICAL");
        }
    }

    private GlobalEventResponse.EventData toEventData(GlobalEventEntity entity) {
        return new GlobalEventResponse.EventData(
                entity.getId(),
                entity.getEventType().name(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getSourceUrl(),
                entity.getSourceName(),
                entity.getRiskLevel().name(),
                entity.getPublishedAt()
        );
    }
}
