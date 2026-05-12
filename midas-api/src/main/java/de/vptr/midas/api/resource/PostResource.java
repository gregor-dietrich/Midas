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
import jakarta.validation.constraints.Min;
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
    @Path("/{id}")
    public Response getPost(@PathParam("id") @Min(1) final Long id) {
        return ResponseUtil.okOrNotFound(this.postService.findById(id));
    }

    /** Returns published posts by user with pagination. */
    @GET
    @Path("/user/{userId}")
    // TODO: add an admin-only path that returns all posts (including drafts) for a user.
    public List<PostResponseDto> getPostsByUser(@PathParam("userId") @Min(1) final Long userId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.findPublishedByUserId(userId, safePage, safeSize);
    }

    /** Returns published posts by category with pagination. */
    @GET
    @Path("/category/{categoryId}")
    public List<PostResponseDto> getPostsByCategory(@PathParam("categoryId") @Min(1) final Long categoryId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.findByCategoryId(categoryId, safePage, safeSize);
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
    // TODO: add an admin-only path that returns all posts (including drafts) within a date range.
    public List<PostResponseDto> getPostsByDateRange(@QueryParam("startDate") final LocalDate startDate,
            @QueryParam("endDate") final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        return this.postService.findPublishedByDateRange(startDate, endDate);
    }

    @POST
    @RolesAllowed({ "post:add" })
    public Response createPost(@Valid final PostDto postDto) {
        final PostResponseDto created = this.postService.createPost(postDto);
        return ResponseUtil.created(created);
    }

    /** Updates the post with the given id. */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ "post:edit" })
    public Response updatePost(@PathParam("id") @Min(1) final Long id, @Valid final PostDto postDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostResponseDto updated = this.postService.updatePost(id, postDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Partially updates the post with the given id. */
    @PATCH
    @Path("/{id}")
    @RolesAllowed({ "post:edit" })
    public Response patchPost(@PathParam("id") @Min(1) final Long id, final PostDto postDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostResponseDto updated = this.postService.patchPost(id, postDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the post with the given id. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "post:delete" })
    public Response deletePost(@PathParam("id") @Min(1) final Long id) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final boolean deleted = this.postService.deletePost(id, currentUsername);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
