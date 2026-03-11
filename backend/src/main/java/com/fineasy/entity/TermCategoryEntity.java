package com.fineasy.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "term_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_term_category_name", columnNames = "name"))
public class TermCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "display_order")
    private Integer displayOrder;

    protected TermCategoryEntity() {
    }

    public TermCategoryEntity(Long id, String name, Integer displayOrder) {
        this.id = id;
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getDisplayOrder() { return displayOrder; }
}
