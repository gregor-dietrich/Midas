package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostCategoryClient;
import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Service for post category operations. */
@ApplicationScoped
public class PostCategoryService {

    private static final Logger LOG = Logger.getLogger(PostCategoryService.class);

    @Inject
    @RestClient
    PostCategoryClient categoryClient;

    @Inject
    AuthService authService;

    /** Returns the all categories. */
    public List<PostCategoryResponseDto> getAllCategories() {
        LOG.debug("Fetching all categories");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper
                .execute(
                        () -> this.categoryClient.getAllCategories(PaginationParamUtil.DEFAULT_PAGE,
                                PaginationParamUtil.MAX_SIZE, authHeader),
                        this.authService, LOG, "fetching categories");
    }

    /** Returns the root categories. */
    public List<PostCategoryResponseDto> getRootCategories() {
        LOG.debug("Fetching root categories");
        return ServiceHelper.execute(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.categoryClient.getRootCategories(authHeader);
        }, this.authService, LOG, "fetching root categories");
    }

    /** Returns the category by id. */
    public Optional<PostCategoryResponseDto> getCategoryById(final Long id) {
        LOG.debugf("Fetching category with ID: %s", id);
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.categoryClient.getCategory(id, authHeader);
        }, this.authService, LOG, "fetching category " + id, response -> ServiceHelper.readOptionalEntity(response,
                PostCategoryResponseDto.class, dto -> dto, "category response"));
    }

    /** Returns the categories by parent. */
    public List<PostCategoryResponseDto> getCategoriesByParent(final Long parentId) {
        LOG.debugf("Fetching categories for parent: %s", parentId);
        return ServiceHelper.execute(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.categoryClient.getCategoriesByParent(parentId, authHeader);
        }, this.authService, LOG, "fetching categories for parent " + parentId);
    }

    /** Creates a new category. */
    public PostCategoryDto createCategory(final PostCategoryDto category) {
        LOG.debugf("Creating new category: %s", category.getName());
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();

            if (category.getParent() != null && category.getParentId() == null) {
                category.setParentId(category.getParent().getId());
            }

            return this.categoryClient.createCategory(category, authHeader);
        }, this.authService, LOG, "creating category", "Failed to create category",
                response -> ServiceHelper.readExpectedStatusEntity(response, 201, PostCategoryResponseDto.class,
                        this::fromApiResponse, "created category response", "Failed to create category"));
    }

    /** Updates the category. */
    public PostCategoryDto updateCategory(final PostCategoryDto category) {
        LOG.debugf("Updating category: %s", category.getId());
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();

            if (category.getParent() != null && category.getParentId() == null) {
                category.setParentId(category.getParent().getId());
            }

            return this.categoryClient.updateCategory(category.getId(), category, authHeader);
        }, this.authService, LOG, "updating category " + category.getId(), "Failed to update category",
                response -> ServiceHelper.readExpectedStatusEntity(response, 200, PostCategoryResponseDto.class,
                        this::fromApiResponse, "updated category response", "Failed to update category"));
    }

    /** Handles the delete category operation. */
    public boolean deleteCategory(final Long id) {
        LOG.debugf("Deleting category: %s", id);
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.categoryClient.deleteCategory(id, authHeader);
        }, this.authService, LOG, "deleting category " + id, response -> response.getStatus() == 204);
    }

    /**
     * Convert API response back to GUI format
     */
    private PostCategoryDto fromApiResponse(final PostCategoryResponseDto responseDto) {
        return responseDto.toPostCategoryDto();
    }

    /** Handles the search categories operation. */
    public List<PostCategoryResponseDto> searchCategories(final String query) {
        LOG.debugf("Searching categories with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.categoryClient.searchCategories(query, authHeader), this.authService,
                LOG, "searching categories");
    }
}
