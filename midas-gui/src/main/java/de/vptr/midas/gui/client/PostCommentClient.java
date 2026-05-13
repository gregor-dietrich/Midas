package de.vptr.midas.gui.client;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.UiConstants;
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
@Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
        abortOn = AuthenticationException.class)
@Timeout(2000)
public interface PostCommentClient {

    @GET
    List<PostCommentResponseDto> getAllComments(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}")
    Response getComment(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/post/{postPublicId}")
    List<PostCommentResponseDto> getCommentsByPost(@PathParam("postPublicId") String postPublicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/user/{userPublicId}")
    List<PostCommentResponseDto> getCommentsByUser(@PathParam("userPublicId") String userPublicId,
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
    @Path("/{publicId}")
    Response updateComment(@PathParam("publicId") String publicId, PostCommentDto comment,
            @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{publicId}")
    Response patchComment(@PathParam("publicId") String publicId, PostCommentDto comment,
            @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{publicId}")
    Response deleteComment(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);
}
