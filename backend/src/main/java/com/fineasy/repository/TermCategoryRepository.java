package com.fineasy.repository;

import com.fineasy.entity.TermCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermCategoryRepository extends JpaRepository<TermCategoryEntity, Long> {

    List<TermCategoryEntity> findAllByOrderByDisplayOrderAsc();
}
