package de.vptr.midas.gui.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
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
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PostCategoryClient {

        @GET
        List<PostCategoryResponseDto> getAllCategories(@QueryParam("page") Integer page,
                        @QueryParam("size") Integer size, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/root")
        List<PostCategoryResponseDto> getRootCategories(@HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}")
        Response getCategory(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/parent/{parentId}")
        List<PostCategoryResponseDto> getCategoriesByParent(@PathParam("parentId") Long parentId,
                        @HeaderParam("Authorization") String authorization);

        @POST
        Response createCategory(PostCategoryDto category, @HeaderParam("Authorization") String authorization);

        @PUT
        @Path("/{id}")
        Response updateCategory(@PathParam("id") Long id, PostCategoryDto category,
                        @HeaderParam("Authorization") String authorization);

        @PATCH
        @Path("/{id}")
        Response patchCategory(@PathParam("id") Long id, PostCategoryDto category,
                        @HeaderParam("Authorization") String authorization);

        @DELETE
        @Path("/{id}")
        Response deleteCategory(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/search")
        List<PostCategoryResponseDto> searchCategories(@QueryParam("query") String query,
                        @HeaderParam("Authorization") String authorization);
}
