package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostClient;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import de.vptr.midas.gui.util.UiConstants;
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
    @SuppressWarnings("NullAway")
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> getAllPosts() {
        return this.getAllPosts(null, null);
    }

    /** Returns the all posts paginated. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> getAllPosts(final Integer page, final Integer size) {
        LOG.debugf("Fetching all posts page %s size %s", page, size);
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.postClient.getAllPosts(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching posts");
    }

    /** Returns the published posts. */
    @SuppressWarnings("NullAway")
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPublishedPosts() {
        return this.getPublishedPosts(null, null);
    }

    /** Returns the published posts paginated. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPublishedPosts(final Integer page, final Integer size) {
        LOG.debugf("Fetching published posts page %s size %s", page, size);
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.postClient.getPublishedPosts(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching published posts");
    }

    /** Returns the post by id. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public Optional<PostResponseDto> getPostById(final Long id) {
        LOG.debugf("Fetching post with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.postClient.getPost(id, authHeader), this.authService, LOG,
                "fetching post " + id, response -> {
                    if (response.getStatus() == 200) {
                        return Optional.of(ServiceHelper.readEntity(response, PostResponseDto.class, "post response"));
                    } else if (response.getStatus() == 404) {
                        return Optional.empty();
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    /** Returns the posts by user. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPostsByUser(final Long userId) {
        LOG.debugf("Fetching posts for user: %s", userId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.postClient.getPostsByUser(userId, authHeader), this.authService, LOG,
                "fetching posts for user " + userId);
    }

    /** Returns the posts by category. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPostsByCategory(final Long categoryId) {
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
                LOG, "creating post", "Failed to create post", response -> {
                    if (response.getStatus() == 201) {
                        return ServiceHelper.readEntity(response, PostDto.class, "created post response");
                    }
                    throw ServiceHelper.responseFailure("Failed to create post", response);
                });
    }

    /** Updates the post. */
    public PostDto updatePost(final PostDto post) {
        LOG.debugf("Updating post: %s", post.getId());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.postClient.updatePost(post.getId(), post, authHeader),
                this.authService, LOG, "updating post " + post.getId(), "Failed to update post", response -> {
                    if (response.getStatus() == 200) {
                        return ServiceHelper.readEntity(response, PostDto.class, "updated post response");
                    }
                    throw ServiceHelper.responseFailure("Failed to update post", response);
                });
    }

    /** Handles the delete post operation. */
    public void deletePost(final Long id) {
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
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> searchPosts(final String query) {
        LOG.debugf("Searching posts with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.postClient.searchPosts(query, authHeader), this.authService, LOG,
                "searching posts");
    }

    /** Returns the posts by date range. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPostsByDateRange(final String startDate, final String endDate) {
        LOG.debugf("Fetching posts by date range: %s to %s", startDate, endDate);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.postClient.getPostsByDateRange(startDate, endDate, authHeader),
                this.authService, LOG, "fetching posts by date range");
    }
}
