package de.vptr.midas.gui.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostClient;
import de.vptr.midas.gui.client.PostClient.DeletePostRequest;
import de.vptr.midas.gui.client.PostClient.GetAllPostsRequest;
import de.vptr.midas.gui.client.PostClient.GetPostRequest;
import de.vptr.midas.gui.client.PostClient.GetPostsByCategoryRequest;
import de.vptr.midas.gui.client.PostClient.GetPostsByDateRangeRequest;
import de.vptr.midas.gui.client.PostClient.GetPostsByUserRequest;
import de.vptr.midas.gui.client.PostClient.GetPublishedPostsRequest;
import de.vptr.midas.gui.client.PostClient.SearchPostsRequest;
import de.vptr.midas.gui.client.PostClient.UpdatePostRequest;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.postClient.getAllPosts(GetAllPostsRequest.newInstance().page(safePage).size(safeSize)),
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
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.postClient
                        .getPublishedPosts(GetPublishedPostsRequest.newInstance().categoryPublicId(categoryPublicId)
                                .startDate(startDate).endDate(endDate).page(safePage).query(query).size(safeSize)),
                this.authService, LOG, "fetching published posts");
    }

    /** Returns the post by id, or empty if it does not exist. */
    public Optional<PostResponseDto> getPostById(final String id) {
        LOG.debugf("Fetching post with ID: %s", id);
        return ServiceHelper.executeOptional(() -> this.postClient.getPost(GetPostRequest.newInstance().publicId(id)),
                this.authService, LOG, "fetching post " + id);
    }

    /** Returns the posts by user. */
    public List<PostResponseDto> getPostsByUser(final String userId) {
        LOG.debugf("Fetching posts for user: %s", userId);
        return ServiceHelper.execute(
                () -> this.postClient
                        .getPostsByUser(GetPostsByUserRequest.newInstance().userPublicId(userId).page(null).size(null)),
                this.authService, LOG, "fetching posts for user " + userId);
    }

    /** Returns the posts by category. */
    public List<PostResponseDto> getPostsByCategory(final String categoryId) {
        LOG.debugf("Fetching posts for category: %s", categoryId);
        return ServiceHelper.execute(
                () -> this.postClient.getPostsByCategory(
                        GetPostsByCategoryRequest.newInstance().categoryPublicId(categoryId).page(null).size(null)),
                this.authService, LOG, "fetching posts for category " + categoryId);
    }

    /** Creates a new post. */
    public PostDto createPost(final PostDto post) {
        LOG.debugf("Creating new post: %s", post.getTitle());
        return PostDto.from(ServiceHelper.execute(() -> this.postClient.createPost(post), this.authService, LOG,
                "creating post", "Failed to create post"));
    }

    /** Updates the post. */
    public PostDto updatePost(final PostDto post) {
        final String publicId = Objects.requireNonNull(post.getPublicId(), "post.publicId");
        LOG.debugf("Updating post: %s", publicId);
        return PostDto.from(ServiceHelper.execute(
                () -> this.postClient.updatePost(UpdatePostRequest.newInstance().publicId(publicId), post),
                this.authService, LOG, "updating post " + publicId, "Failed to update post"));
    }

    /** Handles the delete post operation. */
    public void deletePost(final String id) {
        LOG.debugf("Deleting post: %s", id);
        ServiceHelper.executeVoid(() -> this.postClient.deletePost(DeletePostRequest.newInstance().publicId(id)),
                this.authService, LOG, "deleting post " + id, "Post not found");
        LOG.infof("Successfully deleted post: %s", id);
    }

    /** Handles the search posts operation. */
    public List<PostResponseDto> searchPosts(final String query) {
        LOG.debugf("Searching posts with query: %s", query);
        return ServiceHelper.execute(() -> this.postClient.searchPosts(SearchPostsRequest.newInstance().query(query)),
                this.authService, LOG, "searching posts");
    }

    /** Returns the posts by date range. */
    public List<PostResponseDto> getPostsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        LOG.debugf("Fetching posts by date range: %s to %s", startDate, endDate);
        return ServiceHelper.execute(
                () -> this.postClient.getPostsByDateRange(
                        GetPostsByDateRangeRequest.newInstance().startDate(startDate).endDate(endDate)),
                this.authService, LOG, "fetching posts by date range");
    }
}
