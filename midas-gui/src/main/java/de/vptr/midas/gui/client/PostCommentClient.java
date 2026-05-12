package de.vptr.midas.gui.client;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostCommentResponseDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Component. */
@RegisterRestClient(configKey = "midas-api")
@Path("/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PostCommentClient {

    @GET
    List<PostCommentResponseDto> getAllComments(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}")
    Response getComment(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/post/{postId}")
    List<PostCommentResponseDto> getCommentsByPost(@PathParam("postId") Long postId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/user/{userId}")
    List<PostCommentResponseDto> getCommentsByUser(@PathParam("userId") Long userId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/recent")
    List<PostCommentResponseDto> getRecentComments(@QueryParam("limit") int limit,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<PostCommentResponseDto> searchComments(@QueryParam("query") String query,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/date-range")
    List<PostCommentResponseDto> getCommentsByDateRange(@QueryParam("startDate") LocalDate startDate,
            @QueryParam("endDate") LocalDate endDate, @HeaderParam("Authorization") String authorization);

    @POST
    Response createComment(PostCommentDto comment, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{id}")
    Response updateComment(@PathParam("id") Long id, PostCommentDto comment,
            @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}")
    Response patchComment(@PathParam("id") Long id, PostCommentDto comment,
            @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{id}")
    Response deleteComment(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);
}
