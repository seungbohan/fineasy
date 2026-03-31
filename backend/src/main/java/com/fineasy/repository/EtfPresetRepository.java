package com.fineasy.repository;

import com.fineasy.entity.EtfPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtfPresetRepository extends JpaRepository<EtfPresetEntity, Long> {

    Optional<EtfPresetEntity> findByTicker(String ticker);

    boolean existsByTicker(String ticker);
}
