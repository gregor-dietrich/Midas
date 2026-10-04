package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostCategoryClient;
import de.vptr.midas.gui.client.PostCategoryClient.DeleteCategoryRequest;
import de.vptr.midas.gui.client.PostCategoryClient.GetAllCategoriesRequest;
import de.vptr.midas.gui.client.PostCategoryClient.GetCategoriesByParentRequest;
import de.vptr.midas.gui.client.PostCategoryClient.GetCategoryRequest;
import de.vptr.midas.gui.client.PostCategoryClient.SearchCategoriesRequest;
import de.vptr.midas.gui.client.PostCategoryClient.UpdateCategoryRequest;
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
        return ServiceHelper.execute(
                () -> this.categoryClient.getAllCategories(GetAllCategoriesRequest.newInstance()
                        .page(PaginationParamUtil.DEFAULT_PAGE).size(PaginationParamUtil.MAX_SIZE)),
                this.authService, LOG, "fetching categories");
    }

    /** Returns the root categories. */
    public List<PostCategoryResponseDto> getRootCategories() {
        LOG.debug("Fetching root categories");
        return ServiceHelper.execute(() -> this.categoryClient.getRootCategories(), this.authService, LOG,
                "fetching root categories");
    }

    /** Returns the category by id, or empty if it does not exist. */
    public Optional<PostCategoryResponseDto> getCategoryById(final String id) {
        LOG.debugf("Fetching category with ID: %s", id);
        return ServiceHelper.executeOptional(
                () -> this.categoryClient.getCategory(GetCategoryRequest.newInstance().publicId(id)), this.authService,
                LOG, "fetching category " + id);
    }

    /** Returns the categories by parent. */
    public List<PostCategoryResponseDto> getCategoriesByParent(final String parentId) {
        LOG.debugf("Fetching categories for parent: %s", parentId);
        return ServiceHelper.execute(
                () -> this.categoryClient
                        .getCategoriesByParent(GetCategoriesByParentRequest.newInstance().parentPublicId(parentId)),
                this.authService, LOG, "fetching categories for parent " + parentId);
    }

    /** Creates a new category. */
    public PostCategoryDto createCategory(final PostCategoryDto category) {
        LOG.debugf("Creating new category: %s", category.getName());
        return PostCategoryDto.from(ServiceHelper.execute(() -> {
            final var parent = category.getParent();
            if (parent != null && category.getParentPublicId() == null) {
                category.setParentPublicId(parent.getPublicId());
            }

            return this.categoryClient.createCategory(category);
        }, this.authService, LOG, "creating category", "Failed to create category"));
    }

    /** Updates the category. */
    public PostCategoryDto updateCategory(final PostCategoryDto category) {
        final String publicId = Objects.requireNonNull(category.getPublicId(), "category.publicId");
        LOG.debugf("Updating category: %s", publicId);
        return PostCategoryDto.from(ServiceHelper.execute(() -> {
            final PostCategoryDto parent = category.getParent();
            if (parent != null && category.getParentPublicId() == null) {
                category.setParentPublicId(parent.getPublicId());
            }

            return this.categoryClient.updateCategory(UpdateCategoryRequest.newInstance().publicId(publicId), category);
        }, this.authService, LOG, "updating category " + publicId, "Failed to update category"));
    }

    /** Handles the delete category operation. */
    public void deleteCategory(final String id) {
        LOG.debugf("Deleting category: %s", id);
        ServiceHelper.executeVoid(
                () -> this.categoryClient.deleteCategory(DeleteCategoryRequest.newInstance().publicId(id)),
                this.authService, LOG, "deleting category " + id);
    }

    /** Handles the search categories operation. */
    public List<PostCategoryResponseDto> searchCategories(final String query) {
        LOG.debugf("Searching categories with query: %s", query);
        return ServiceHelper.execute(
                () -> this.categoryClient.searchCategories(SearchCategoriesRequest.newInstance().query(query)),
                this.authService, LOG, "searching categories");
    }
}
