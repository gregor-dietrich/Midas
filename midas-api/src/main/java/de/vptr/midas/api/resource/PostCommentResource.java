package de.vptr.midas.api.resource;

import java.util.List;

import de.vptr.midas.api.dto.PostCommentDto;
import de.vptr.midas.api.dto.PostCommentResponseDto;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.service.PostCommentService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import de.vptr.midas.api.util.SecurityContextUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/** REST resource for managing post comments. */
@Path("/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PostCommentResource {

    @Context
    SecurityContext securityContext;

    @Inject
    PostCommentService commentService;

    /** Returns all comments with normalized pagination parameters. */
    @GET
    @RolesAllowed({ "post-comment:edit", "post-comment:delete" })
    public List<PostCommentResponseDto> getAllComments(
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.commentService.getAllComments(safePage, safeSize);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response getComment(@PathParam("id") @Min(1) final Long id) {
        return ResponseUtil.okOrNotFound(this.commentService.findById(id));
    }

    @GET
    @Path("/post/{postId}")
    @PermitAll
    public List<PostCommentResponseDto> getCommentsByPost(@PathParam("postId") @Min(1) final Long postId) {
        return this.commentService.findByPostId(postId);
    }

    @GET
    @Path("/user/{userId}")
    @Authenticated
    public List<PostCommentResponseDto> getCommentsByUser(@PathParam("userId") @Min(1) final Long userId) {
        return this.commentService.findByUserId(userId);
    }

    @GET
    @Path("/recent")
    @PermitAll
    public List<PostCommentResponseDto> getRecentComments(@QueryParam("limit") @DefaultValue("10") final int limit) {
        final int safeLimit = PaginationUtil.sanitizeRecentLimit(limit);
        return this.commentService.findRecentComments(safeLimit);
    }

    /** Returns all comments matching the search query. */
    @GET
    @Path("/search")
    @PermitAll
    public List<PostCommentResponseDto> searchComments(@QueryParam("query") final String query) {
        if (query != null && query.length() > 100) {
            throw new WebApplicationException("Search query too long", Response.Status.BAD_REQUEST);
        }
        if (query == null || query.trim().isEmpty()) {
            return this.commentService.getAllComments();
        }
        return this.commentService.searchComments(query);
    }

    /** Returns comments within the given date range. */
    @GET
    @Path("/date-range")
    public List<PostCommentResponseDto> getCommentsByDateRange(
            @QueryParam("startDate") final String startDate,
            @QueryParam("endDate") final String endDate) {
        return this.commentService.findByDateRange(startDate, endDate);
    }

    /** Processes the request and returns the result. */
    @POST
    @RolesAllowed({ "post-comment:add" })
    public Response createComment(@Valid final PostCommentDto commentDto) {
        // Map DTO to entity
        final PostCommentEntity comment = new PostCommentEntity();
        comment.content = commentDto.content;
        if (commentDto.postId != null) {
            comment.post = PostEntity.findById(commentDto.postId);
            if (comment.post == null) {
                return ResponseUtil.notFound();
            }
        }
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostCommentResponseDto created = this.commentService.createComment(comment, currentUsername);
        return ResponseUtil.created(created);
    }

    /** Processes the request and returns the result. */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ "post-comment:edit" })
    public Response updateComment(@PathParam("id") @Min(1) final Long id, @Valid final PostCommentDto commentDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        // Map DTO to entity
        final PostCommentEntity comment = new PostCommentEntity();
        comment.id = id;
        comment.content = commentDto.content;

        final PostCommentResponseDto updated = this.commentService.updateComment(comment, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @PATCH
    @Path("/{id}")
    @RolesAllowed({ "post-comment:edit" })
    public Response patchComment(@PathParam("id") @Min(1) final Long id, @Valid final PostCommentDto commentDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        // Map DTO to entity
        final PostCommentEntity comment = new PostCommentEntity();
        comment.id = id;
        comment.content = commentDto.content;

        final PostCommentResponseDto updated = this.commentService.patchComment(comment, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "post-comment:delete" })
    public Response deleteComment(@PathParam("id") @Min(1) final Long id) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final boolean deleted = this.commentService.deleteComment(id, currentUsername);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
