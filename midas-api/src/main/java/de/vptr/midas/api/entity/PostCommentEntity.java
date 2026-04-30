package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a post comment. */
@Entity
@Table(name = "post_comments", indexes = {
        @Index(name = "idx_post_comment_post_id", columnList = "post_id"),
        @Index(name = "idx_post_comment_user_id", columnList = "user_id")
})
public class PostCommentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    public PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final PostCommentEntity that = (PostCommentEntity) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
