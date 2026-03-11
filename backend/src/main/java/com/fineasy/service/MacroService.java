package com.fineasy.service;

import com.fineasy.dto.response.MacroIndicatorResponse;
import com.fineasy.entity.MacroIndicatorEntity;
import com.fineasy.exception.EntityNotFoundException;
import com.fineasy.external.ecos.EcosIndicatorDef;
import com.fineasy.external.fred.FredIndicatorDef;
import com.fineasy.repository.MacroIndicatorRepository;
import com.fineasy.util.ChangeCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MacroService {

    private static final Logger log = LoggerFactory.getLogger(MacroService.class);

    private final MacroIndicatorRepository macroIndicatorRepository;

    public MacroService(MacroIndicatorRepository macroIndicatorRepository) {
        this.macroIndicatorRepository = macroIndicatorRepository;
    }

    public List<MacroIndicatorEntity> getLatestIndicatorEntities() {
        return macroIndicatorRepository.findLatestIndicators();
    }

    @Cacheable(value = "macro-indicators", unless = "#result == null || #result.isEmpty()")
    public List<MacroIndicatorResponse> getLatestIndicators() {
        List<MacroIndicatorEntity> latestList = macroIndicatorRepository.findLatestIndicators();

        Map<String, Double> previousValueMap = buildPreviousValueMap();

        return latestList.stream()
                .map(indicator -> toResponseWithChange(indicator, previousValueMap))
                .toList();
    }

    public List<MacroIndicatorResponse> getIndicatorHistory(String indicatorCode) {
        List<MacroIndicatorEntity> indicators = macroIndicatorRepository
                .findByCodeOrderByDateDesc(indicatorCode, PageRequest.of(0, 30));

        if (indicators.isEmpty()) {
            throw new EntityNotFoundException("MacroIndicator", indicatorCode);
        }

        return indicators.stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "macro-indicators-category", key = "#categoryName.toUpperCase()",
            unless = "#result == null || #result.isEmpty()")
    public List<MacroIndicatorResponse> getIndicatorsByCategory(String categoryName) {
        FredIndicatorDef.Category category;
        try {
            category = FredIndicatorDef.Category.valueOf(categoryName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid category: " + categoryName
                    + ". Valid categories: POLICY, ECONOMY, FINANCIAL_MARKET, LIQUIDITY, COMMODITY, FOREX");
        }

        List<String> codes = new ArrayList<>();
        codes.addAll(FredIndicatorDef.codesByCategory(category));
        codes.addAll(EcosIndicatorDef.codesByCategory(category));

        if (category == FredIndicatorDef.Category.COMMODITY) {
            codes.addAll(List.of("GOLD", "SILVER", "WTI"));
        } else if (category == FredIndicatorDef.Category.FINANCIAL_MARKET) {
            codes.add("US_DXY");
        }
        if (codes.isEmpty()) {
            return List.of();
        }

        List<MacroIndicatorEntity> latestList =
                macroIndicatorRepository.findLatestByIndicatorCodes(codes);

        Map<String, Double> previousValueMap = buildPreviousValueMapByCodes(codes);

        return latestList.stream()
                .map(indicator -> toResponseWithChange(indicator, previousValueMap))
                .toList();
    }

    private MacroIndicatorResponse toResponse(MacroIndicatorEntity indicator) {
        return new MacroIndicatorResponse(
                indicator.getId(),
                indicator.getIndicatorCode(),
                indicator.getIndicatorName(),
                indicator.getValue(),
                indicator.getUnit(),
                indicator.getRecordDate(),
                indicator.getSource()
        );
    }

    private MacroIndicatorResponse toResponseWithChange(MacroIndicatorEntity indicator,
                                                         Map<String, Double> previousValueMap) {
        Double previousValue = previousValueMap.get(indicator.getIndicatorCode());
        ChangeCalculator.Change change = ChangeCalculator.calculate(indicator.getValue(), previousValue);

        return new MacroIndicatorResponse(
                indicator.getId(),
                indicator.getIndicatorCode(),
                indicator.getIndicatorName(),
                indicator.getValue(),
                indicator.getUnit(),
                indicator.getRecordDate(),
                indicator.getSource(),
                change.changeAmount(),
                change.changeRate()
        );
    }

    private Map<String, Double> buildPreviousValueMap() {
        try {
            return macroIndicatorRepository.findPreviousIndicators().stream()
                    .collect(Collectors.toMap(
                            MacroIndicatorEntity::getIndicatorCode,
                            MacroIndicatorEntity::getValue,
                            (v1, v2) -> v1
                    ));
        } catch (Exception e) {
            log.warn("Failed to load previous indicator values for change calculation: {}",
                    e.getMessage());
            return Map.of();
        }
    }

    private Map<String, Double> buildPreviousValueMapByCodes(List<String> codes) {
        try {
            return macroIndicatorRepository.findPreviousByIndicatorCodes(codes).stream()
                    .collect(Collectors.toMap(
                            MacroIndicatorEntity::getIndicatorCode,
                            MacroIndicatorEntity::getValue,
                            (v1, v2) -> v1
                    ));
        } catch (Exception e) {
            log.warn("Failed to load previous indicator values for category change calculation: {}",
                    e.getMessage());
            return Map.of();
        }
    }
}
