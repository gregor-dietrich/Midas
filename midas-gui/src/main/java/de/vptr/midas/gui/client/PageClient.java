package de.vptr.midas.gui.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
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
@Path("/pages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PageClient {

    @GET
    List<PageResponseDto> getAllPages(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}")
    Response getPage(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<PageResponseDto> searchPages(@QueryParam("query") String searchTerm,
            @HeaderParam("Authorization") String authorization);

    @POST
    Response createPage(PageDto page, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{id}")
    Response updatePage(@PathParam("id") Long id, PageDto page, @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}")
    Response patchPage(@PathParam("id") Long id, PageDto page, @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{id}")
    Response deletePage(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);
}
