package de.vptr.midas.gui.client;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
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
@Path("/user-groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
        abortOn = AuthenticationException.class)
@Timeout(2000)
public interface UserGroupClient {

    @GET
    List<UserGroupResponseDto> getAllGroups(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}")
    Response getGroup(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/name/{name}")
    Response getGroupByName(@PathParam("name") String name, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}/users")
    List<UserDto> getUsersInGroup(@PathParam("publicId") String groupPublicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/user/{userPublicId}")
    List<UserGroupResponseDto> getGroupsForUser(@PathParam("userPublicId") String userPublicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/user/{userPublicId}/group/{groupPublicId}/check")
    Response checkUserInGroup(@PathParam("userPublicId") String userPublicId,
            @PathParam("groupPublicId") String groupPublicId, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<UserGroupResponseDto> searchGroups(@QueryParam("query") String query,
            @HeaderParam("Authorization") String authorization);

    @POST
    Response createGroup(UserGroupDto group, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{publicId}")
    Response updateGroup(@PathParam("publicId") String publicId, UserGroupDto group,
            @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{publicId}")
    Response patchGroup(@PathParam("publicId") String publicId, UserGroupDto group,
            @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{publicId}")
    Response deleteGroup(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @POST
    @Path("/{groupPublicId}/users/{userPublicId}")
    Response addUserToGroup(@PathParam("groupPublicId") String groupPublicId,
            @PathParam("userPublicId") String userPublicId, @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{groupPublicId}/users/{userPublicId}")
    Response removeUserFromGroup(@PathParam("groupPublicId") String groupPublicId,
            @PathParam("userPublicId") String userPublicId, @HeaderParam("Authorization") String authorization);
}
