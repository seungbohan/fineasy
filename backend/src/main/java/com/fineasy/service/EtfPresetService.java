package com.fineasy.service;

import com.fineasy.dto.response.EtfPresetResponse;
import com.fineasy.entity.EtfPresetEntity;
import com.fineasy.repository.EtfPresetRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EtfPresetService {

    private final EtfPresetRepository etfPresetRepository;

    public EtfPresetService(EtfPresetRepository etfPresetRepository) {
        this.etfPresetRepository = etfPresetRepository;
    }

    @Cacheable(value = "etf-presets", unless = "#result == null")
    public EtfPresetResponse getAllPresets() {
        List<EtfPresetEntity> entities = etfPresetRepository.findAll();

        List<EtfPresetResponse.EtfPresetData> presets = entities.stream()
                .map(this::toPresetData)
                .toList();

        return new EtfPresetResponse(presets);
    }

    private EtfPresetResponse.EtfPresetData toPresetData(EtfPresetEntity entity) {
        return new EtfPresetResponse.EtfPresetData(
                entity.getId(),
                entity.getTicker(),
                entity.getNameEn(),
                entity.getNameKo(),
                entity.getMarket(),
                entity.getCategory(),
                entity.getAnnualReturnRate(),
                entity.getDescription()
        );
    }
}
