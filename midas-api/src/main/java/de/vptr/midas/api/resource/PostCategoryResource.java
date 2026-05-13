package de.vptr.midas.api.resource;

import java.util.List;

import de.vptr.midas.api.dto.PostCategoryDto;
import de.vptr.midas.api.dto.PostCategoryResponseDto;
import de.vptr.midas.api.service.PostCategoryService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST resource for managing post categories. */
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PostCategoryResource {

    @Inject
    PostCategoryService categoryService;

    /** Returns all categories with normalized pagination parameters. */
    @GET
    @Authenticated
    public List<PostCategoryResponseDto> getAllCategories(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.categoryService.getAllCategories(safePage, safeSize);
    }

    @GET
    @Path("/root")
    @Authenticated
    public List<PostCategoryResponseDto> getRootCategories() {
        return this.categoryService.findRootCategories();
    }

    /** Returns the category with the given publicId. */
    @GET
    @Path("/{publicId}")
    @Authenticated
    public Response getCategory(@PathParam("publicId") final String publicId) {
        return this.categoryService.findById(publicId).map(ResponseUtil::ok).orElse(ResponseUtil.notFound());
    }

    @GET
    @Path("/parent/{parentPublicId}")
    @Authenticated
    public List<PostCategoryResponseDto>
            getCategoriesByParent(@PathParam("parentPublicId") final String parentPublicId) {
        return this.categoryService.findByParentPublicId(parentPublicId);
    }

    /** Creates a new post category. */
    @POST
    @RolesAllowed({ "post-category:add" })
    public Response createCategory(@Valid final PostCategoryDto categoryDto) {
        final var responseDto = this.categoryService.createCategory(categoryDto);
        return ResponseUtil.created(responseDto);
    }

    /** Updates the category with the given publicId. */
    @PUT
    @Path("/{publicId}")
    @RolesAllowed({ "post-category:edit" })
    public Response updateCategory(@PathParam("publicId") final String publicId,
            @Valid final PostCategoryDto categoryDto) {
        final var responseDto = this.categoryService.updateCategory(publicId, categoryDto);
        return ResponseUtil.ok(responseDto);
    }

    /** Partially updates the category with the given publicId. */
    @PATCH
    @Path("/{publicId}")
    @RolesAllowed({ "post-category:edit" })
    public Response patchCategory(@PathParam("publicId") final String publicId, final PostCategoryDto categoryDto) {
        final var responseDto = this.categoryService.patchCategory(publicId, categoryDto);
        return ResponseUtil.ok(responseDto);
    }

    /** Deletes the category with the given publicId. */
    @DELETE
    @Path("/{publicId}")
    @RolesAllowed({ "post-category:delete" })
    public Response deleteCategory(@PathParam("publicId") final String publicId) {
        final var deleted = this.categoryService.deleteCategory(publicId);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    @GET
    @Path("/search")
    @RolesAllowed({ "post-category:add", "post-category:edit", "post-category:delete" })
    public List<PostCategoryResponseDto> searchCategories(@QueryParam("query") final String query) {
        return this.categoryService.searchCategories(query);
    }
}
