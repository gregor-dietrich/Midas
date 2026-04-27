package de.vptr.midas.api.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a user rank with permission flags. */
@Entity
@Table(name = "user_ranks")
public class UserRankEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(unique = true)
    public String name;

    // View permissions
    @Column(name = "admin_view")
    public Boolean adminView = false;

    // Page permissions
    @Column(name = "page_add")
    public Boolean pageAdd = false;

    @Column(name = "page_delete")
    public Boolean pageDelete = false;

    @Column(name = "page_edit")
    public Boolean pageEdit = false;

    // Post permissions
    @Column(name = "post_add")
    public Boolean postAdd = false;

    @Column(name = "post_delete")
    public Boolean postDelete = false;

    @Column(name = "post_edit")
    public Boolean postEdit = false;

    // Post category permissions
    @Column(name = "post_category_add")
    public Boolean postCategoryAdd = false;

    @Column(name = "post_category_delete")
    public Boolean postCategoryDelete = false;

    @Column(name = "post_category_edit")
    public Boolean postCategoryEdit = false;

    // Post comment permissions
    @Column(name = "post_comment_add")
    public Boolean postCommentAdd = false;

    @Column(name = "post_comment_delete")
    public Boolean postCommentDelete = false;

    @Column(name = "post_comment_edit")
    public Boolean postCommentEdit = false;

    // User permissions
    @Column(name = "user_add")
    public Boolean userAdd = false;

    @Column(name = "user_delete")
    public Boolean userDelete = false;

    @Column(name = "user_edit")
    public Boolean userEdit = false;

    // User group permissions
    @Column(name = "user_group_add")
    public Boolean userGroupAdd = false;

    @Column(name = "user_group_delete")
    public Boolean userGroupDelete = false;

    @Column(name = "user_group_edit")
    public Boolean userGroupEdit = false;

    // Account permissions
    @Column(name = "user_account_add")
    public Boolean userAccountAdd = false;

    @Column(name = "user_account_delete")
    public Boolean userAccountDelete = false;

    @Column(name = "user_account_edit")
    public Boolean userAccountEdit = false;

    // User rank permissions
    @Column(name = "user_rank_add")
    public Boolean userRankAdd = false;

    @Column(name = "user_rank_delete")
    public Boolean userRankDelete = false;

    @Column(name = "user_rank_edit")
    public Boolean userRankEdit = false;

    @OneToMany(mappedBy = "rank", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JsonIgnore
    public List<UserEntity> users;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UserRankEntity that = (UserRankEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
