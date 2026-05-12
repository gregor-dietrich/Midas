package de.vptr.midas.gui.client;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
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
@Path("/user-ranks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
        abortOn = AuthenticationException.class)
@Timeout(2000)
public interface UserRankClient {

    @GET
    List<UserRankResponseDto> getAllRanks(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}")
    Response getRank(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/name/{name}")
    Response getRankByName(@PathParam("name") String name, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<UserRankResponseDto> searchRanks(@QueryParam("query") String query,
            @HeaderParam("Authorization") String authorization);

    @POST
    Response createRank(UserRankDto rank, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{id}")
    Response updateRank(@PathParam("id") Long id, UserRankDto rank, @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}")
    Response patchRank(@PathParam("id") Long id, UserRankDto rank, @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{id}")
    Response deleteRank(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);
}
