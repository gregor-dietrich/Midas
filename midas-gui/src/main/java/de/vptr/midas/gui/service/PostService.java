package de.vptr.midas.gui.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostClient;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/** Service for post operations. */
@ApplicationScoped
public class PostService {

    private static final Logger LOG = Logger.getLogger(PostService.class);

    @Inject
    @RestClient
    PostClient postClient;

    @Inject
    AuthService authService;

    /** Returns the all posts. */
    public List<PostResponseDto> getAllPosts() {
        return this.getAllPosts(null, null);
    }

    /** Returns the all posts paginated. */
    public List<PostResponseDto> getAllPosts(@Nullable final Integer page, @Nullable final Integer size) {
        LOG.debugf("Fetching all posts page %s size %s", page, size);
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.postClient.getAllPosts(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching posts");
    }

    /** Returns the published posts. */
    public List<PostResponseDto> getPublishedPosts() {
        return this.getPublishedPosts(null, null);
    }

    /** Returns the published posts paginated. */
    public List<PostResponseDto> getPublishedPosts(@Nullable final Integer page, @Nullable final Integer size) {
        return this.getPublishedPosts(null, null, null, null, page, size);
    }

    /**
     * Returns published posts filtered server-side by optional category, created-date range and free-text query, with
     * pagination. Mirrors the payments approach so the GUI never loads the full post set into memory.
     */
    public List<PostResponseDto> getPublishedPosts(@Nullable final String categoryPublicId,
            @Nullable final LocalDate startDate, @Nullable final LocalDate endDate, @Nullable final String query,
            @Nullable final Integer page, @Nullable final Integer size) {
        LOG.debugf("Fetching published posts page %s size %s", page, size);
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.postClient.getPublishedPosts(safePage, safeSize, categoryPublicId,
                startDate, endDate, query, authHeader), this.authService, LOG, "fetching published posts");
    }

    /** Returns the post by id. */
    public Optional<PostResponseDto> getPostById(final String id) {
        LOG.debugf("Fetching post with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.postClient.getPost(id, authHeader), this.authService, LOG,
                "fetching post " + id, response -> ServiceHelper.readOptionalEntity(response, PostResponseDto.class,
                        dto -> dto, "post response"));
    }

    /** Returns the posts by user. */
    public List<PostResponseDto> getPostsByUser(final String userId) {
        LOG.debugf("Fetching posts for user: %s", userId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.postClient.getPostsByUser(userId, authHeader), this.authService, LOG,
                "fetching posts for user " + userId);
    }

    /** Returns the posts by category. */
    public List<PostResponseDto> getPostsByCategory(final String categoryId) {
        LOG.debugf("Fetching posts for category: %s", categoryId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.postClient.getPostsByCategory(categoryId, authHeader), this.authService,
                LOG, "fetching posts for category " + categoryId);
    }

    /** Creates a new post. */
    public PostDto createPost(final PostDto post) {
        LOG.debugf("Creating new post: %s", post.getTitle());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.postClient.createPost(post, authHeader), this.authService,
                LOG, "creating post", "Failed to create post",
                response -> ServiceHelper.readExpectedStatusEntity(response, 201, PostDto.class, dto -> dto,
                        "created post response", "Failed to create post"));
    }

    /** Updates the post. */
    public PostDto updatePost(final PostDto post) {
        final String publicId = Objects.requireNonNull(post.getPublicId(), "post.publicId");
        LOG.debugf("Updating post: %s", publicId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.postClient.updatePost(publicId, post, authHeader),
                this.authService, LOG, "updating post " + publicId, "Failed to update post",
                response -> ServiceHelper.readExpectedStatusEntity(response, 200, PostDto.class, dto -> dto,
                        "updated post response", "Failed to update post"));
    }

    /** Handles the delete post operation. */
    public void deletePost(final String id) {
        LOG.debugf("Deleting post: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        ServiceHelper.executeWithResponse(() -> this.postClient.deletePost(id, authHeader), this.authService, LOG,
                "deleting post " + id, response -> {
                    if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                        LOG.infof("Successfully deleted post: %s", id);
                        return null;
                    } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                        throw new ServiceException("Post not found");
                    }
                    throw ServiceHelper.responseFailure("Failed to delete post", response);
                });
    }

    /** Handles the search posts operation. */
    public List<PostResponseDto> searchPosts(final String query) {
        LOG.debugf("Searching posts with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.postClient.searchPosts(query, authHeader), this.authService, LOG,
                "searching posts");
    }

    /** Returns the posts by date range. */
    public List<PostResponseDto> getPostsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        LOG.debugf("Fetching posts by date range: %s to %s", startDate, endDate);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.postClient.getPostsByDateRange(startDate, endDate, authHeader),
                this.authService, LOG, "fetching posts by date range");
    }
}
