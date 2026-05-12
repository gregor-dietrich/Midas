package de.vptr.midas.gui.client;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserResponseDto;
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
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
        abortOn = AuthenticationException.class)
@Timeout(2000)
public interface UserClient {

    @GET
    List<UserResponseDto> getAllUsers(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/me")
    Response getCurrentUser(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}")
    Response getUser(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/username/{username}")
    Response getUserByUsername(@PathParam("username") String username,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/email/{email}")
    Response getUserByEmail(@PathParam("email") String email, @HeaderParam("Authorization") String authorization);

    @POST
    Response createUser(UserDto user, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{id}")
    Response updateUser(@PathParam("id") Long id, UserDto user, @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}")
    Response patchUser(@PathParam("id") Long id, UserDto user, @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{id}")
    Response deleteUser(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<UserResponseDto> searchUsers(@QueryParam("query") String query,
            @HeaderParam("Authorization") String authorization);
}
