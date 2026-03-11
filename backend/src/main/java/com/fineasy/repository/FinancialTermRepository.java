package com.fineasy.repository;

import com.fineasy.entity.FinancialTermEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FinancialTermRepository extends JpaRepository<FinancialTermEntity, Long> {

    @Query("SELECT t FROM FinancialTermEntity t WHERE " +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<FinancialTermEntity> searchByName(@Param("query") String query);

    List<FinancialTermEntity> findByCategoryId(Long categoryId);

    @Query("SELECT t FROM FinancialTermEntity t ORDER BY t.name ASC")
    List<FinancialTermEntity> findAllOrderByName();
}
