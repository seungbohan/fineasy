package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_watchlist_user_stock",
                columnNames = {"user_id", "stock_id"}),
        indexes = {
                @Index(name = "idx_watchlist_user", columnList = "user_id")
        })
public class WatchlistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected WatchlistEntity() {
    }

    public WatchlistEntity(Long id, UserEntity user, StockEntity stock, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public StockEntity getStock() { return stock; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
