package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a blog post. */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_user_id", columnList = "user_id"),
        @Index(name = "idx_post_category_id", columnList = "category_id")
})
public class PostEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    public PostCategoryEntity category;

    @Column
    public Boolean published = false;

    @Column
    public Boolean commentable = false;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "post", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JsonIgnore
    public List<PostCommentEntity> comments;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final PostEntity that = (PostEntity) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
