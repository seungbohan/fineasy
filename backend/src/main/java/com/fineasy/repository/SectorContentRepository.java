package com.fineasy.repository;

import com.fineasy.entity.SectorContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SectorContentRepository extends JpaRepository<SectorContentEntity, Long> {

    @Query("SELECT s FROM SectorContentEntity s ORDER BY s.nameKo")
    List<SectorContentEntity> findAllOrderByNameKo();

    @Query("SELECT s FROM SectorContentEntity s " +
            "LEFT JOIN FETCH s.companies c " +
            "WHERE s.slug = :slug " +
            "ORDER BY c.displayOrder")
    Optional<SectorContentEntity> findBySlugWithCompanies(@Param("slug") String slug);

    boolean existsBySlug(String slug);
}
