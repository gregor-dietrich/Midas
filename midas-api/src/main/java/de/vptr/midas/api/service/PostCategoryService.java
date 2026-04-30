package de.vptr.midas.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.vptr.midas.api.dto.PostCategoryResponseDto;
import de.vptr.midas.api.entity.PostCategoryEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for post category business logic. */
@ApplicationScoped
public class PostCategoryService {

    private static final int MAX_CATEGORY_DEPTH = 100;

    /** Returns all categories with default pagination. */
    public List<PostCategoryResponseDto> getAllCategories() {
        return this.getAllCategories(0, 20);
    }

    /** Returns all categories with the given pagination. */
    public List<PostCategoryResponseDto> getAllCategories(final int page, final int size) {
        final List<PostCategoryEntity> categories = PostCategoryEntity
                .find("SELECT c FROM PostCategoryEntity c LEFT JOIN FETCH c.parent ORDER BY c.id")
                .page(page, size)
                .list();
        return this.toCategoryResponseDtos(categories);
    }

    /** Returns the category with the given id. */
    public Optional<PostCategoryResponseDto> findById(final Long id) {
        return PostCategoryEntity
                .find("SELECT c FROM PostCategoryEntity c LEFT JOIN FETCH c.parent WHERE c.id = ?1", id)
                .<PostCategoryEntity>firstResultOptional()
                .map(this::toCategoryResponseDto);
    }

    /** Returns all root categories. */
    public List<PostCategoryResponseDto> findRootCategories() {
        final List<PostCategoryEntity> categories = PostCategoryEntity
                .find("SELECT c FROM PostCategoryEntity c WHERE c.parent IS NULL")
                .list();
        return this.toCategoryResponseDtos(categories);
    }

    /** Returns by parent id. */
    public List<PostCategoryResponseDto> findByParentId(final Long parentId) {
        final List<PostCategoryEntity> categories = PostCategoryEntity
                .find("SELECT c FROM PostCategoryEntity c LEFT JOIN FETCH c.parent WHERE c.parent.id = ?1", parentId)
                .list();
        return this.toCategoryResponseDtos(categories);
    }

    /** Creates a new post category. */
    @Transactional
    public PostCategoryResponseDto createCategory(final PostCategoryEntity category) {
        // Validate name is provided for creation
        if (category.name == null || category.name.trim().isEmpty()) {
            throw new ValidationException("Name is required for creating a category");
        }

        // If parent is specified, ensure it exists
        if (category.parent != null && category.parent.id != null) {
            final var existingParent = (PostCategoryEntity) PostCategoryEntity.findById(category.parent.id);
            if (existingParent == null) {
                throw new WebApplicationException("Parent category not found", Response.Status.BAD_REQUEST);
            }
            category.parent = existingParent;
        }

        category.persistAndFlush();
        PostCategoryEntity.getEntityManager().detach(category);
        return this.toCategoryResponseDto(PostCategoryEntity.findById(category.id));
    }

    /** Updates the category with the given id. */
    @Transactional
    public PostCategoryResponseDto updateCategory(final PostCategoryEntity category) {
        final var existingCategory = (PostCategoryEntity) PostCategoryEntity.findById(category.id);
        if (existingCategory == null) {
            throw new WebApplicationException("Category not found", Response.Status.NOT_FOUND);
        }

        // Validate name is provided for complete replacement (PUT)
        if (category.name == null || category.name.trim().isEmpty()) {
            throw new ValidationException("Name is required for updating a category");
        }

        // Complete replacement (PUT semantics) - update name and parent
        existingCategory.name = category.name;

        // Handle parent change - validate if parent is provided
        if (category.parent != null && category.parent.id != null) {
            final PostCategoryEntity newParent = PostCategoryEntity.findById(category.parent.id);
            if (newParent == null) {
                throw new WebApplicationException("Parent category not found", Response.Status.BAD_REQUEST);
            }
            // Prevent circular references
            if (this.isDescendantOf(newParent.id, existingCategory.id)) {
                throw new WebApplicationException("Cannot set parent to a descendant category",
                        Response.Status.BAD_REQUEST);
            }
            existingCategory.parent = newParent;
        } else if (category.parent == null) {
            // Explicitly set to null if parent is null (making it a root category)
            existingCategory.parent = null;
        }

        existingCategory.persistAndFlush();
        PostCategoryEntity.getEntityManager().detach(existingCategory);
        return this.toCategoryResponseDto(PostCategoryEntity.findById(existingCategory.id));
    }

    /** Partially updates the category with the given id. */
    @Transactional
    public PostCategoryResponseDto patchCategory(final PostCategoryEntity category) {
        final var existingCategory = (PostCategoryEntity) PostCategoryEntity.findById(category.id);
        if (existingCategory == null) {
            throw new WebApplicationException("Category not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (category.name != null) {
            existingCategory.name = category.name;
        }

        // Handle parent change if provided
        if (category.parent != null) {
            if (category.parent.id != null) {
                final PostCategoryEntity newParent = (PostCategoryEntity) PostCategoryEntity
                        .findById(category.parent.id);
                if (newParent == null) {
                    throw new WebApplicationException("Parent category not found", Response.Status.BAD_REQUEST);
                }
                // Prevent circular references
                if (this.isDescendantOf(newParent.id, existingCategory.id)) {
                    throw new WebApplicationException("Cannot set parent to a descendant category",
                            Response.Status.BAD_REQUEST);
                }
                existingCategory.parent = newParent;
            } else {
                // Set to null if parent ID is null (making it a root category)
                existingCategory.parent = null;
            }
        }

        existingCategory.persistAndFlush();
        PostCategoryEntity.getEntityManager().detach(existingCategory);
        return this.toCategoryResponseDto(PostCategoryEntity.findById(existingCategory.id));
    }

    /**
     * Check if potential parent is a descendant of the category (to prevent
     * circular references). Uses scalar queries to avoid lazy-loading issues.
     */
    private boolean isDescendantOf(final Long potentialParentId, final Long categoryId) {
        if (potentialParentId.equals(categoryId)) {
            return true;
        }

        // Fetch all parent mappings in one query to prevent O(N) queries
        // Assuming PostCategory table is not massively large
        final List<Object[]> paths = PostCategoryEntity.getEntityManager()
                .createQuery("SELECT c.id, c.parent.id FROM PostCategoryEntity c WHERE c.parent IS NOT NULL",
                        Object[].class)
                .getResultList();

        final Map<Long, Long> parentMap = new HashMap<>();
        for (final Object[] path : paths) {
            parentMap.put((Long) path[0], (Long) path[1]);
        }

        Long currentId = potentialParentId;
        int depth = 0;
        while (currentId != null && depth < MAX_CATEGORY_DEPTH) {
            currentId = parentMap.get(currentId);
            if (currentId != null && currentId.equals(categoryId)) {
                return true;
            }
            depth++;
        }
        return false;
    }

    /** Deletes the category with the given id. */
    @Transactional
    public boolean deleteCategory(final Long id) {
        return PostCategoryEntity.deleteById(id);
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
        final List<Long> childrenIds = PostCategoryEntity.getEntityManager()
                .createQuery("SELECT c.id FROM PostCategoryEntity c WHERE c.parent.id = :parentId", Long.class)
                .setParameter("parentId", category.id)
                .getResultList();
        final int postsCount = (int) PostEntity.count("category.id", category.id);
        return new PostCategoryResponseDto(category, childrenIds.size(), childrenIds, postsCount);
    }

    private List<PostCategoryResponseDto> toCategoryResponseDtos(final List<PostCategoryEntity> categories) {
        if (categories.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = categories.stream().map(c -> c.id).toList();

        // Batch fetch children info
        final List<Object[]> childResults = PostCategoryEntity.getEntityManager()
                .createQuery(
                        "SELECT c.parent.id, c.id FROM PostCategoryEntity c WHERE c.parent.id IN :ids",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList();
        final Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (final Object[] row : childResults) {
            childrenByParent.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add((Long) row[1]);
        }

        // Batch fetch post counts
        final List<Object[]> postCountResults = PostEntity.getEntityManager()
                .createQuery(
                        "SELECT p.category.id, COUNT(p) FROM PostEntity p WHERE p.category.id IN :ids GROUP BY p.category.id",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList();
        final Map<Long, Long> postCounts = new HashMap<>();
        for (final Object[] row : postCountResults) {
            postCounts.put((Long) row[0], (Long) row[1]);
        }

        return categories.stream()
                .map(c -> {
                    final List<Long> childrenIds = childrenByParent.getOrDefault(c.id, List.of());
                    final int postsCount = postCounts.getOrDefault(c.id, 0L).intValue();
                    return new PostCategoryResponseDto(c, childrenIds.size(), childrenIds, postsCount);
                })
                .toList();
    }
}
