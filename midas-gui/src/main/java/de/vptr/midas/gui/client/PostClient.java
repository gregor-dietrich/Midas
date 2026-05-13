package de.vptr.midas.gui.client;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
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
@Path("/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
        abortOn = AuthenticationException.class)
@Timeout(2000)
public interface PostClient {

    @GET
    List<PostResponseDto> getAllPosts(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/published")
    List<PostResponseDto> getPublishedPosts(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}")
    Response getPost(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/user/{userPublicId}")
    List<PostResponseDto> getPostsByUser(@PathParam("userPublicId") String userPublicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/category/{categoryPublicId}")
    List<PostResponseDto> getPostsByCategory(@PathParam("categoryPublicId") String categoryPublicId,
            @HeaderParam("Authorization") String authorization);

    @POST
    Response createPost(PostDto post, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{publicId}")
    Response updatePost(@PathParam("publicId") String publicId, PostDto post,
            @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{publicId}")
    Response patchPost(@PathParam("publicId") String publicId, PostDto post,
            @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{publicId}")
    Response deletePost(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<PostResponseDto> searchPosts(@QueryParam("query") String query,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/date-range")
    List<PostResponseDto> getPostsByDateRange(@QueryParam("startDate") LocalDate startDate,
            @QueryParam("endDate") LocalDate endDate, @HeaderParam("Authorization") String authorization);
}
