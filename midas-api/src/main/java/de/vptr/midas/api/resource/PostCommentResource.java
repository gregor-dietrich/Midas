package de.vptr.midas.api.resource;

import java.time.LocalDate;
import java.util.List;

import de.vptr.midas.api.dto.PostCommentDto;
import de.vptr.midas.api.dto.PostCommentResponseDto;
import de.vptr.midas.api.service.PostCommentService;
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
    @Nullable
    SecurityContext securityContext;

    @Inject
    PostCommentService commentService;

    /** Returns all comments with normalized pagination parameters. */
    @GET
    @RolesAllowed({ "post-comment:edit", "post-comment:delete" })
    public List<PostCommentResponseDto> getAllComments(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.commentService.getAllComments(safePage, safeSize);
    }

    @GET
    @Path("/{publicId}")
    public Response getComment(@PathParam("publicId") final String publicId) {
        return ResponseUtil.okOrNotFound(this.commentService.findById(publicId));
    }

    /** Returns comments for a post with pagination. */
    @GET
    @Path("/post/{postPublicId}")
    public List<PostCommentResponseDto> getCommentsByPost(@PathParam("postPublicId") final String postPublicId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.commentService.findByPostPublicId(postPublicId, safePage, safeSize);
    }

    /** Returns comments by user with pagination. */
    @GET
    @Path("/user/{userPublicId}")
    @Authenticated
    public List<PostCommentResponseDto> getCommentsByUser(@PathParam("userPublicId") final String userPublicId,
            @QueryParam("page") @DefaultValue("0") final int pageIdx,
            @QueryParam("size") @DefaultValue("20") final int pageSize) {
        final int safePage = PaginationUtil.sanitizePage(pageIdx);
        final int safeSize = PaginationUtil.sanitizeSize(pageSize);
        return this.commentService.findByUserPublicId(userPublicId, safePage, safeSize);
    }

    @GET
    @Path("/recent")
    public List<PostCommentResponseDto> getRecentComments(@QueryParam("limit") @DefaultValue("10") final int limit) {
        final int safeLimit = PaginationUtil.sanitizeRecentLimit(limit);
        return this.commentService.findRecentComments(safeLimit);
    }

    /** Returns all comments matching the search query. */
    @GET
    @Path("/search")
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
    public List<PostCommentResponseDto> getCommentsByDateRange(@QueryParam("startDate") final LocalDate startDate,
            @QueryParam("endDate") final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }
        return this.commentService.findByDateRange(startDate, endDate);
    }

    /** Processes the request and returns the result. */
    @POST
    @RolesAllowed({ "post-comment:add" })
    public Response createComment(@Valid final PostCommentDto commentDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostCommentResponseDto created = this.commentService.createComment(commentDto, currentUsername);
        return ResponseUtil.created(created);
    }

    /** Processes the request and returns the result. */
    @PUT
    @Path("/{publicId}")
    @RolesAllowed({ "post-comment:edit" })
    public Response updateComment(@PathParam("publicId") final String publicId,
            @Valid final PostCommentDto commentDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostCommentResponseDto updated = this.commentService.updateComment(publicId, commentDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @PATCH
    @Path("/{publicId}")
    @RolesAllowed({ "post-comment:edit" })
    public Response patchComment(@PathParam("publicId") final String publicId, final PostCommentDto commentDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PostCommentResponseDto updated = this.commentService.patchComment(publicId, commentDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @DELETE
    @Path("/{publicId}")
    @RolesAllowed({ "post-comment:delete" })
    public Response deleteComment(@PathParam("publicId") final String publicId) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final boolean deleted = this.commentService.deleteComment(publicId, currentUsername);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
