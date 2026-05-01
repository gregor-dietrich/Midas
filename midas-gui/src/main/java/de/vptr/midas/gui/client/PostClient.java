package de.vptr.midas.gui.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
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
public interface PostClient {

        @GET
        List<PostResponseDto> getAllPosts(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/published")
        List<PostResponseDto> getPublishedPosts(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}")
        Response getPost(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/user/{userId}")
        List<PostResponseDto> getPostsByUser(@PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/category/{categoryId}")
        List<PostResponseDto> getPostsByCategory(@PathParam("categoryId") Long categoryId,
                        @HeaderParam("Authorization") String authorization);

        @POST
        Response createPost(PostDto post, @HeaderParam("Authorization") String authorization);

        @PUT
        @Path("/{id}")
        Response updatePost(@PathParam("id") Long id, PostDto post, @HeaderParam("Authorization") String authorization);

        @PATCH
        @Path("/{id}")
        Response patchPost(@PathParam("id") Long id, PostDto post, @HeaderParam("Authorization") String authorization);

        @DELETE
        @Path("/{id}")
        Response deletePost(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/search")
        List<PostResponseDto> searchPosts(@QueryParam("query") String query,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/date-range")
        List<PostResponseDto> getPostsByDateRange(
                        @QueryParam("startDate") String startDate,
                        @QueryParam("endDate") String endDate,
                        @HeaderParam("Authorization") String authorization);
}
