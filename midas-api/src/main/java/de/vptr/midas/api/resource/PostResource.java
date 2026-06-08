package de.vptr.midas.api.resource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    /**
     * Returns posts with normalized pagination. Non-privileged callers receive published posts only; callers with
     * {@code admin:view} receive all posts including drafts.
     */
    @GET
    public List<PostResponseDto> getAllPosts(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.isAdminView() ? this.postService.getAllPosts(safePage, safeSize)
                : this.postService.findPublishedPosts(safePage, safeSize);
    }

    /**
     * Returns published posts with normalized pagination, optionally filtered server-side by category, created-date
     * range and free-text query so the GUI never materializes the full post set.
     */
    @GET
    @Path("/published")
    public List<PostResponseDto> getPublishedPosts(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size,
            @QueryParam("categoryPublicId") @Nullable final String categoryPublicId,
            @QueryParam("startDate") @Nullable final LocalDate startDate,
            @QueryParam("endDate") @Nullable final LocalDate endDate,
            @QueryParam("query") @Nullable final String query) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.postService.findPublishedPosts(categoryPublicId, startDate, endDate, query, safePage, safeSize);
    }

    /** Returns the post with the given publicId, hiding unpublished drafts from non-privileged callers. */
    @GET
    @Path("/{publicId}")
    public Response getPost(@PathParam("publicId") final String publicId) {
        final Optional<PostResponseDto> post = this.postService.findById(publicId);
        // Hide unpublished drafts from non-privileged callers (the author and admins may still see them).
        if (post.isPresent() && !this.mayViewPost(post.get())) {
            return ResponseUtil.notFound();
        }
        return ResponseUtil.okOrNotFound(post);
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

    /**
     * Returns posts by category with pagination. Non-privileged callers receive published posts only; callers with
     * {@code admin:view} receive all posts including drafts.
     */
    @GET
    @Path("/category/{categoryPublicId}")
    public List<PostResponseDto> getPostsByCategory(@PathParam("categoryPublicId") final String categoryPublicId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.isAdminView() ? this.postService.findByCategoryPublicId(categoryPublicId, safePage, safeSize)
                : this.postService.findPublishedPosts(categoryPublicId, null, null, null, safePage, safeSize);
    }

    /**
     * Returns posts matching the search query. Non-privileged callers search published posts only; callers with
     * {@code admin:view} search all posts including drafts.
     */
    @GET
    @Path("/search")
    public List<PostResponseDto> searchPosts(@QueryParam("query") final String query) {
        final boolean admin = this.isAdminView();
        if (query == null || query.trim().isEmpty()) {
            return admin ? this.postService.getAllPosts() : this.postService.findPublishedPosts();
        }
        return admin ? this.postService.searchPosts(query) : this.postService.searchPublishedPosts(query);
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

    /** Returns true when the caller holds the {@code admin:view} role. */
    private boolean isAdminView() {
        return this.securityContext != null && this.securityContext.isUserInRole("admin:view");
    }

    /** A post is viewable when it is published, or the caller is an admin, or the caller is its author. */
    private boolean mayViewPost(final PostResponseDto post) {
        if (Boolean.TRUE.equals(post.published) || this.isAdminView()) {
            return true;
        }
        final String username = this.currentUsernameOrNull();
        return username != null && username.equals(post.username);
    }

    @Nullable
    private String currentUsernameOrNull() {
        if (this.securityContext == null) {
            return null;
        }
        final var principal = this.securityContext.getUserPrincipal();
        return principal != null ? principal.getName() : null;
    }
}
