package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a post comment. */
@Entity
@Table(name = "post_comments")
public class PostCommentEntity extends BaseEntity {

    @Column(columnDefinition = "TEXT")
    @NotBlank
    @Nullable
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @Nullable
    public PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @Nullable
    public UserEntity user;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    // Helper method to find comments by post
    public static List<PostCommentEntity> findByPostId(final Long postId) {
        return find("post.id", postId).list();
    }

    // Helper method to find comments by user
    public static List<PostCommentEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    // Helper method to find recent comments
    public static List<PostCommentEntity> findRecentComments(final int limit) {
        return find("ORDER BY created DESC").page(0, limit).list();
    }
}
