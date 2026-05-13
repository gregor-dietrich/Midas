package de.vptr.midas.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.vptr.midas.api.dto.PostCategoryDto;
import de.vptr.midas.api.dto.PostCategoryResponseDto;
import de.vptr.midas.api.entity.PostCategoryEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for post category business logic. */
@ApplicationScoped
public class PostCategoryService {

    /** Returns all categories with default pagination. */
    public List<PostCategoryResponseDto> getAllCategories() {
        return this.getAllCategories(0, 20);
    }

    /** Returns all categories with the given pagination. */
    public List<PostCategoryResponseDto> getAllCategories(final int page, final int size) {
        final List<PostCategoryEntity> categories =
                PostCategoryEntity.find("SELECT c FROM PostCategoryEntity c LEFT JOIN FETCH c.parent ORDER BY c.id")
                        .page(page, size).list();
        return this.toCategoryResponseDtos(categories);
    }

    /** Returns the category with the given publicId. */
    public Optional<PostCategoryResponseDto> findById(final String publicId) {
        return PostCategoryEntity
                .find("SELECT c FROM PostCategoryEntity c LEFT JOIN FETCH c.parent WHERE c.publicId = ?1", publicId)
                .<PostCategoryEntity>firstResultOptional().map(this::toCategoryResponseDto);
    }

    /** Returns all root categories. */
    public List<PostCategoryResponseDto> findRootCategories() {
        final List<PostCategoryEntity> categories =
                PostCategoryEntity.find("SELECT c FROM PostCategoryEntity c WHERE c.parent IS NULL").list();
        return this.toCategoryResponseDtos(categories);
    }

    /** Returns by parent publicId. */
    public List<PostCategoryResponseDto> findByParentPublicId(final String parentPublicId) {
        final List<PostCategoryEntity> categories = PostCategoryEntity
                .find("SELECT c FROM PostCategoryEntity c LEFT JOIN FETCH c.parent WHERE c.parent.publicId = ?1",
                        parentPublicId)
                .list();
        return this.toCategoryResponseDtos(categories);
    }

    /** Creates a new post category. */
    @Transactional
    public PostCategoryResponseDto createCategory(final PostCategoryDto dto) {
        final PostCategoryEntity category = new PostCategoryEntity();
        category.name = dto.name;
        this.applyParent(category, dto.parentPublicId, false);
        category.persistAndFlush();
        return this.toCategoryResponseDto(category);
    }

    /** Updates the category with the given publicId. */
    @Transactional
    public PostCategoryResponseDto updateCategory(final String publicId, final PostCategoryDto dto) {
        final PostCategoryEntity existingCategory = PostCategoryEntity.find("publicId", publicId).firstResult();
        if (existingCategory == null) {
            throw new WebApplicationException("Category not found", Response.Status.NOT_FOUND);
        }
        existingCategory.name = dto.name;
        this.applyParent(existingCategory, dto.parentPublicId, true);
        existingCategory.persistAndFlush();
        return this.toCategoryResponseDto(existingCategory);
    }

    /** Partially updates the category with the given publicId. */
    @Transactional
    public PostCategoryResponseDto patchCategory(final String publicId, final PostCategoryDto dto) {
        final PostCategoryEntity existingCategory = PostCategoryEntity.find("publicId", publicId).firstResult();
        if (existingCategory == null) {
            throw new WebApplicationException("Category not found", Response.Status.NOT_FOUND);
        }
        if (dto.name != null) {
            existingCategory.name = dto.name;
        }
        if (dto.parentPublicId != null) {
            this.applyParent(existingCategory, dto.parentPublicId, false);
        }
        existingCategory.persistAndFlush();
        return this.toCategoryResponseDto(existingCategory);
    }

    private void applyParent(final PostCategoryEntity category, @Nullable final String parentPublicId,
            final boolean clearIfNull) {
        if (parentPublicId != null) {
            final PostCategoryEntity newParent = PostCategoryEntity.find("publicId", parentPublicId).firstResult();
            if (newParent == null) {
                throw new WebApplicationException("Parent category not found", Response.Status.BAD_REQUEST);
            }
            if (newParent.id != null && category.id != null && this.isDescendantOf(newParent.id, category.id)) {
                throw new WebApplicationException("Cannot set parent to a descendant category",
                        Response.Status.BAD_REQUEST);
            }
            category.parent = newParent;
        } else if (clearIfNull) {
            category.parent = null;
        }
    }

    /**
     * Check if potential parent is a descendant of the category (to prevent circular references). Uses a recursive CTE
     * to traverse ancestors.
     */
    private boolean isDescendantOf(final Long potentialParentId, final Long categoryId) {
        if (potentialParentId.equals(categoryId)) {
            return true;
        }

        @SuppressWarnings("unchecked")
        final List<Long> ancestors = PostCategoryEntity.getEntityManager().createNativeQuery("""
                WITH RECURSIVE ancestors AS (
                    SELECT id, parent_id FROM post_categories WHERE id = :startId
                    UNION ALL
                    SELECT c.id, c.parent_id FROM post_categories c
                    INNER JOIN ancestors a ON c.id = a.parent_id
                )
                SELECT id FROM ancestors
                """).setParameter("startId", potentialParentId).getResultList();

        return ancestors.contains(categoryId);
    }

    /** Deletes the category with the given publicId. */
    @Transactional
    public boolean deleteCategory(final String publicId) {
        return PostCategoryEntity.delete("publicId", publicId) > 0;
    }

    /** Returns all categories matching the given query. */
    public List<PostCategoryResponseDto> searchCategories(final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllCategories();
        }
        final List<PostCategoryEntity> categories = SearchQueryUtil.applyDefaultSearchLimit(PostCategoryEntity.find(
                "SELECT c FROM PostCategoryEntity c LEFT JOIN FETCH c.parent WHERE LOWER(c.name) LIKE ?1 ESCAPE '\\'",
                searchTerm)).list();
        return this.toCategoryResponseDtos(categories);
    }

    private PostCategoryResponseDto toCategoryResponseDto(final PostCategoryEntity category) {
        final List<String> childrenPublicIds = PostCategoryEntity.getEntityManager()
                .createQuery("SELECT c.publicId FROM PostCategoryEntity c WHERE c.parent.id = :parentId", String.class)
                .setParameter("parentId", category.id).getResultList();
        final int postsCount = (int) PostEntity.count("category.id", category.id);
        return new PostCategoryResponseDto(category, childrenPublicIds.size(), childrenPublicIds, postsCount);
    }

    private List<PostCategoryResponseDto> toCategoryResponseDtos(final List<PostCategoryEntity> categories) {
        if (categories.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = categories.stream().map(c -> c.id).toList();

        // Batch fetch children info
        final List<Object[]> childResults = PostCategoryEntity.getEntityManager()
                .createQuery("SELECT c.parent.id, c.publicId FROM PostCategoryEntity c WHERE c.parent.id IN :ids",
                        Object[].class)
                .setParameter("ids", ids).getResultList();
        final Map<Long, List<String>> childrenByParent = new HashMap<>();
        for (final Object[] row : childResults) {
            childrenByParent.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add((String) row[1]);
        }

        // Batch fetch post counts
        final List<Object[]> postCountResults = PostEntity.getEntityManager().createQuery(
                "SELECT p.category.id, COUNT(p) FROM PostEntity p WHERE p.category.id IN :ids GROUP BY p.category.id",
                Object[].class).setParameter("ids", ids).getResultList();
        final Map<Long, Long> postCounts = new HashMap<>();
        for (final Object[] row : postCountResults) {
            postCounts.put((Long) row[0], (Long) row[1]);
        }

        return categories.stream().map(c -> {
            final List<String> childrenPublicIds = childrenByParent.getOrDefault(c.id, List.of());
            final int postsCount = postCounts.getOrDefault(c.id, 0L).intValue();
            return new PostCategoryResponseDto(c, childrenPublicIds.size(), childrenPublicIds, postsCount);
        }).toList();
    }
}
