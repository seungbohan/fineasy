package com.fineasy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_post_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_user_reaction", columnNames = {"post_id", "user_id"})
})
public class StockPostReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private StockPostEntity post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 10)
    private ReactionType reactionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockPostReactionEntity() {
    }

    public StockPostReactionEntity(StockPostEntity post, Long userId, ReactionType reactionType) {
        this.post = post;
        this.userId = userId;
        this.reactionType = reactionType;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void changeReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public Long getId() { return id; }
    public StockPostEntity getPost() { return post; }
    public Long getUserId() { return userId; }
    public ReactionType getReactionType() { return reactionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
