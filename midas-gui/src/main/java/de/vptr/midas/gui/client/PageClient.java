package de.vptr.midas.gui.client;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
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
@Path("/pages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
        abortOn = AuthenticationException.class)
@Timeout(2000)
public interface PageClient {

    @GET
    List<PageResponseDto> getAllPages(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}")
    Response getPage(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<PageResponseDto> searchPages(@QueryParam("query") String searchTerm,
            @HeaderParam("Authorization") String authorization);

    @POST
    Response createPage(PageDto page, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{publicId}")
    Response updatePage(@PathParam("publicId") String publicId, PageDto page,
            @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{publicId}")
    Response patchPage(@PathParam("publicId") String publicId, PageDto page,
            @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{publicId}")
    Response deletePage(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);
}
