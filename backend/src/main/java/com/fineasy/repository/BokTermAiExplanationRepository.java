package com.fineasy.repository;

import com.fineasy.entity.BokTermAiExplanationEntity;
import com.fineasy.entity.BokTermEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BokTermAiExplanationRepository extends JpaRepository<BokTermAiExplanationEntity, Long> {

    Optional<BokTermAiExplanationEntity> findByBokTerm(BokTermEntity bokTerm);
}
