package de.vptr.midas.api.resource;

import java.time.LocalDate;
import java.util.List;

import de.vptr.midas.api.dto.PostDto;
import de.vptr.midas.api.dto.PostResponseDto;
import de.vptr.midas.api.service.PostService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import de.vptr.midas.api.util.SecurityContextUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.Nullable;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/** REST resource for managing posts. */
@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PostResource {

    @Inject
    PostService postService;

    @Context
    @Nullable
    SecurityContext securityContext;

    /** Returns all posts with normalized pagination parameters. */
    @GET
    public List<PostResponseDto> getAllPosts(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.getAllPosts(safePage, safeSize);
    }

    /** Returns published posts with normalized pagination parameters. */
    @GET
    @Path("/published")
    public List<PostResponseDto> getPublishedPosts(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.findPublishedPosts(safePage, safeSize);
    }

    @GET
    @Path("/{publicId}")
    public Response getPost(@PathParam("publicId") final String publicId) {
        return ResponseUtil.okOrNotFound(this.postService.findById(publicId));
    }

    /** Returns published posts by user with pagination. */
    @GET
    @Path("/user/{userPublicId}")
    public List<PostResponseDto> getPostsByUser(@PathParam("userPublicId") final String userPublicId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.findPublishedByUserPublicId(userPublicId, safePage, safeSize);
    }

    /** Returns all posts (including drafts) by user with pagination. Admin-only. */
    @GET
    @Path("/user/{userPublicId}/admin")
    @RolesAllowed({ "admin:view" })
    public List<PostResponseDto> getAllPostsByUser(@PathParam("userPublicId") final String userPublicId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.findByUserPublicId(userPublicId, safePage, safeSize);
    }

    /** Returns published posts by category with pagination. */
    @GET
    @Path("/category/{categoryPublicId}")
    public List<PostResponseDto> getPostsByCategory(@PathParam("categoryPublicId") final String categoryPublicId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.findByCategoryPublicId(categoryPublicId, safePage, safeSize);
    }

    /** Returns all posts matching the search query. */
    @GET
    @Path("/search")
    public List<PostResponseDto> searchPosts(@QueryParam("query") final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.postService.getAllPosts();
        }
        return this.postService.searchPosts(query);
    }

    /** Returns posts within the given date range. */
    @GET
    @Path("/date-range")
    public List<PostResponseDto> getPostsByDateRange(@QueryParam("startDate") final LocalDate startDate,
            @QueryParam("endDate") final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        return this.postService.findPublishedByDateRange(startDate, endDate);
    }

    /** Returns all posts (including drafts) within the given date range. Admin-only. */
    @GET
    @Path("/date-range/admin")
    @RolesAllowed({ "admin:view" })
    public List<PostResponseDto> getAllPostsByDateRange(@QueryParam("startDate") final LocalDate startDate,
            @QueryParam("endDate") final LocalDate endDate) {
        return this.postService.findByDateRange(startDate, endDate);
    }

    /**
     * Creates a new post.
     *
     * @param postDto
     *            the post data
     * @return the created post
     */
    @POST
    @RolesAllowed({ "post:add" })
    public Response createPost(@Valid final PostDto postDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostResponseDto created = this.postService.createPost(postDto, currentUsername);
        return ResponseUtil.created(created);
    }

    /** Updates the post with the given publicId. */
    @PUT
    @Path("/{publicId}")
    @RolesAllowed({ "post:edit" })
    public Response updatePost(@PathParam("publicId") final String publicId, @Valid final PostDto postDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostResponseDto updated = this.postService.updatePost(publicId, postDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Partially updates the post with the given publicId. */
    @PATCH
    @Path("/{publicId}")
    @RolesAllowed({ "post:edit" })
    public Response patchPost(@PathParam("publicId") final String publicId, final PostDto postDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostResponseDto updated = this.postService.patchPost(publicId, postDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the post with the given publicId. */
    @DELETE
    @Path("/{publicId}")
    @RolesAllowed({ "post:delete" })
    public Response deletePost(@PathParam("publicId") final String publicId) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final boolean deleted = this.postService.deletePost(publicId, currentUsername);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
