package com.fineasy.repository;

import com.fineasy.entity.DartCorpCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DartCorpCodeRepository extends JpaRepository<DartCorpCodeEntity, Long> {

    Optional<DartCorpCodeEntity> findByStockCode(String stockCode);
}
