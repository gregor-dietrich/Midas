package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostCategoryClient;
import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Component. */
@Timeout(value = 5, unit = ChronoUnit.SECONDS)
@ApplicationScoped
public class PostCategoryService {

    private static final Logger LOG = Logger.getLogger(PostCategoryService.class);

    @Inject
    @RestClient
    PostCategoryClient categoryClient;

    @Inject
    AuthService authService;

    /** Returns the all categories. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostCategoryResponseDto> getAllCategories() {
        LOG.debug("Fetching all categories");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.categoryClient.getAllCategories(PaginationParamUtil.DEFAULT_PAGE,
                        PaginationParamUtil.MAX_SIZE, authHeader),
                this.authService, LOG, "fetching categories");
    }

    /** Returns the root categories. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostCategoryResponseDto> getRootCategories() {
        LOG.debug("Fetching root categories");
        return ServiceHelper.execute(() -> {
        final var authHeader = this.authService.requireAuthorizationHeader();
            return this.categoryClient.getRootCategories(authHeader);
        }, this.authService, LOG, "fetching root categories");
    }

    /** Returns the category by id. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<PostCategoryResponseDto> getCategoryById(final Long id) {
        LOG.debugf("Fetching category with ID: %s", id);
        return ServiceHelper.executeWithResponse(() -> {
        final var authHeader = this.authService.requireAuthorizationHeader();
            return this.categoryClient.getCategory(id, authHeader);
        }, this.authService, LOG, "fetching category " + id, response -> {
            if (response.getStatus() == 200) {
                return Optional
                        .of(ServiceHelper.readEntity(response, PostCategoryResponseDto.class, "category response"));
            } else if (response.getStatus() == 404) {
                return Optional.empty();
            } else {
                throw ServiceHelper.backendError(response);
            }
        });
    }

    /** Returns the categories by parent. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
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
        }, this.authService, LOG, "creating category", "Failed to create category", response -> {
            if (response.getStatus() == 201) {
                final PostCategoryResponseDto responseDto = ServiceHelper.readEntity(response,
                        PostCategoryResponseDto.class, "created category response");
                return this.fromApiResponse(responseDto);
            }
            throw ServiceHelper.responseFailure("Failed to create category", response);
        });
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
        }, this.authService, LOG, "updating category " + category.getId(), "Failed to update category", response -> {
            if (response.getStatus() == 200) {
                final PostCategoryResponseDto responseDto = ServiceHelper.readEntity(response,
                        PostCategoryResponseDto.class, "updated category response");
                return this.fromApiResponse(responseDto);
            }
            throw ServiceHelper.responseFailure("Failed to update category", response);
        });
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
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostCategoryResponseDto> searchCategories(final String query) {
        LOG.debugf("Searching categories with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.categoryClient.searchCategories(query, authHeader),
                this.authService, LOG, "searching categories");
    }
}
