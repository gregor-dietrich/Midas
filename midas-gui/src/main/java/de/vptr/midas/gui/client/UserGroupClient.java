package de.vptr.midas.gui.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
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
public interface UserGroupClient {

        @GET
        List<UserGroupResponseDto> getAllGroups(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}")
        Response getGroup(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/name/{name}")
        Response getGroupByName(@PathParam("name") String name, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}/users")
        List<UserDto> getUsersInGroup(@PathParam("id") Long groupId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/user/{userId}")
        List<UserGroupResponseDto> getGroupsForUser(@PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/user/{userId}/group/{groupId}/check")
        Response checkUserInGroup(@PathParam("userId") Long userId, @PathParam("groupId") Long groupId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/search")
        List<UserGroupResponseDto> searchGroups(@QueryParam("query") String query,
                        @HeaderParam("Authorization") String authorization);

        @POST
        Response createGroup(UserGroupDto group, @HeaderParam("Authorization") String authorization);

        @PUT
        @Path("/{id}")
        Response updateGroup(@PathParam("id") Long id, UserGroupDto group,
                        @HeaderParam("Authorization") String authorization);

        @PATCH
        @Path("/{id}")
        Response patchGroup(@PathParam("id") Long id, UserGroupDto group,
                        @HeaderParam("Authorization") String authorization);

        @DELETE
        @Path("/{id}")
        Response deleteGroup(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @POST
        @Path("/{groupId}/users/{userId}")
        Response addUserToGroup(@PathParam("groupId") Long groupId, @PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @DELETE
        @Path("/{groupId}/users/{userId}")
        Response removeUserFromGroup(@PathParam("groupId") Long groupId, @PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);
}
