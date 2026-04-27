package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostClient;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Component. */
@Timeout(value = 5, unit = ChronoUnit.SECONDS)
@ApplicationScoped
public class PostService {

    private static final Logger LOG = Logger.getLogger(PostService.class);

    @Inject
    @RestClient
    PostClient postClient;

    @Inject
    AuthService authService;

    /** Returns the all posts. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostResponseDto> getAllPosts(final String authHeader) {
        LOG.debug("Fetching all posts");
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.postClient.getAllPosts(authHeader),
                this.authService, LOG, "fetching posts");
    }

    /** Returns the published posts. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPublishedPosts(final String authHeader) {
        LOG.debug("Fetching published posts with provided auth header");
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.postClient.getPublishedPosts(authHeader),
                this.authService, LOG, "fetching published posts");
    }

    /** Returns the post by id. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<PostResponseDto> getPostById(final Long id) {
        LOG.debugf("Fetching post with ID: %s", id);
        final var authHeader = this.authService.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> this.postClient.getPost(id, authHeader),
                this.authService, LOG, "fetching post " + id, response -> {
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
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPostsByUser(final Long userId) {
        LOG.debugf("Fetching posts for user: %s", userId);
        final var authHeader = this.authService.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.postClient.getPostsByUser(userId, authHeader),
                this.authService, LOG, "fetching posts for user " + userId);
    }

    /** Returns the posts by category. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPostsByCategory(final Long categoryId) {
        LOG.debugf("Fetching posts for category: %s", categoryId);
        final var authHeader = this.authService.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.postClient.getPostsByCategory(categoryId, authHeader),
                this.authService, LOG, "fetching posts for category " + categoryId);
    }

    /** Creates a new post. */
    public PostDto createPost(final PostDto post) {
        LOG.debugf("Creating new post: %s", post.getTitle());
        final var authHeader = this.authService.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> this.postClient.createPost(post, authHeader),
                this.authService, LOG, "creating post", "Failed to create post", response -> {
                    if (response.getStatus() == 201) {
                        return ServiceHelper.readEntity(response, PostDto.class, "created post response");
                    }
                    throw ServiceHelper.responseFailure("Failed to create post", response);
                });
    }

    /** Updates the post. */
    public PostDto updatePost(final PostDto post) {
        LOG.debugf("Updating post: %s", post.getId());
        final var authHeader = this.authService.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> this.postClient.updatePost(post.getId(), post, authHeader),
                this.authService, LOG, "updating post " + post.getId(), "Failed to update post", response -> {
                    if (response.getStatus() == 200) {
                        return ServiceHelper.readEntity(response, PostDto.class, "updated post response");
                    }
                    throw ServiceHelper.responseFailure("Failed to update post", response);
                });
    }

    /** Handles the delete post operation. */
    public boolean deletePost(final Long id) {
        LOG.debugf("Deleting post: %s", id);
        final var authHeader = this.authService.getAuthorizationHeader();
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.executeWithResponse(() -> this.postClient.deletePost(id, authHeader),
                this.authService, LOG, "deleting post " + id, response -> response.getStatus() == 204);
    }

    /** Handles the search posts operation. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostResponseDto> searchPosts(final String query, final String authHeader) {
        LOG.debugf("Searching posts with query: %s", query);
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.postClient.searchPosts(query, authHeader),
                this.authService, LOG, "searching posts");
    }

    /** Returns the posts by date range. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PostResponseDto> getPostsByDateRange(final String startDate, final String endDate,
            final String authHeader) {
        LOG.debugf("Fetching posts by date range: %s to %s", startDate, endDate);
        if (authHeader == null) {
            LOG.warn("No authentication header available");
            throw new AuthenticationException("Authentication required");
        }
        return ServiceHelper.execute(
                () -> this.postClient.getPostsByDateRange(startDate, endDate, authHeader),
                this.authService, LOG, "fetching posts by date range");
    }
}
