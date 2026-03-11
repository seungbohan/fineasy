package com.fineasy.repository;

import com.fineasy.entity.BokTermEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BokTermRepository extends JpaRepository<BokTermEntity, Long> {

    @Query("SELECT t FROM BokTermEntity t WHERE " +
            "LOWER(t.term) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.englishTerm) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.definition) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<BokTermEntity> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT * FROM bok_terms ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    BokTermEntity findRandomTerm();

    @Query("SELECT t FROM BokTermEntity t WHERE t.term IN :terms")
    List<BokTermEntity> findByTermIn(@Param("terms") List<String> terms);

    Page<BokTermEntity> findAllByOrderByTermAsc(Pageable pageable);
}
