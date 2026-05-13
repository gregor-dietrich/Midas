package de.vptr.midas.api.resource;

import java.util.List;

import de.vptr.midas.api.dto.UserGroupDto;
import de.vptr.midas.api.dto.UserGroupResponseDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.service.UserGroupService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST resource for managing user groups. */
@Path("/user-groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserGroupResource {

    @Inject
    UserGroupService groupService;

    /** Returns all groups with normalized pagination parameters. */
    @GET
    @RolesAllowed({ "user-group:edit", "user-group:delete" })
    public List<UserGroupResponseDto> getAllGroups(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.groupService.getAllGroups(safePage, safeSize);
    }

    @GET
    @Path("/{publicId}")
    public Response getGroup(@PathParam("publicId") final String publicId) {
        return ResponseUtil.okOrNotFound(this.groupService.findById(publicId));
    }

    @GET
    @Path("/name/{name}")
    public Response getGroupByName(@PathParam("name") @Size(max = 255) final String name) {
        return ResponseUtil.okOrNotFound(this.groupService.findByName(name));
    }

    @GET
    @Path("/{publicId}/users")
    @RolesAllowed({ "user-group:edit", "user-group:delete" })
    public List<UserResponseDto> getUsersInGroup(@PathParam("publicId") final String publicId) {
        return this.groupService.getUsersInGroup(publicId);
    }

    @GET
    @Path("/user/{userPublicId}")
    @RolesAllowed({ "user-group:edit", "user-group:delete" })
    public List<UserGroupResponseDto> getGroupsForUser(@PathParam("userPublicId") final String userPublicId) {
        return this.groupService.getGroupsForUser(userPublicId);
    }

    @GET
    @Path("/user/{userPublicId}/group/{groupPublicId}/check")
    @RolesAllowed({ "user-group:edit", "user-group:delete" })
    public Response checkUserInGroup(@PathParam("userPublicId") final String userPublicId,
            @PathParam("groupPublicId") final String groupPublicId) {
        final boolean isInGroup = this.groupService.isUserInGroup(userPublicId, groupPublicId);
        return ResponseUtil.ok(isInGroup);
    }

    /** Returns all groups matching the search query. */
    @GET
    @Path("/search")
    public List<UserGroupResponseDto> searchGroups(@QueryParam("query") final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.groupService.getAllGroups();
        }
        return this.groupService.searchGroups(query);
    }

    @POST
    @RolesAllowed({ "user-group:add" })
    public Response createGroup(@Valid final UserGroupDto groupDto) {
        final UserGroupResponseDto created = this.groupService.createGroup(groupDto);
        return ResponseUtil.created(created);
    }

    @PUT
    @Path("/{publicId}")
    @RolesAllowed({ "user-group:edit" })
    public Response updateGroup(@PathParam("publicId") final String publicId, @Valid final UserGroupDto groupDto) {
        final UserGroupResponseDto updated = this.groupService.updateGroup(publicId, groupDto);
        return ResponseUtil.ok(updated);
    }

    @PATCH
    @Path("/{publicId}")
    @RolesAllowed({ "user-group:edit" })
    public Response patchGroup(@PathParam("publicId") final String publicId, final UserGroupDto groupDto) {
        final UserGroupResponseDto updated = this.groupService.patchGroup(publicId, groupDto);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the group with the given publicId. */
    @DELETE
    @Path("/{publicId}")
    @RolesAllowed({ "user-group:delete" })
    public Response deleteGroup(@PathParam("publicId") final String publicId) {
        final boolean deleted = this.groupService.deleteGroup(publicId);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    @POST
    @Path("/{groupPublicId}/users/{userPublicId}")
    @RolesAllowed({ "user-group:edit" })
    public Response addUserToGroup(@PathParam("groupPublicId") final String groupPublicId,
            @PathParam("userPublicId") final String userPublicId) {
        this.groupService.addUserToGroup(userPublicId, groupPublicId);
        return ResponseUtil.created(null);
    }

    /** Removes a user from a group. */
    @DELETE
    @Path("/{groupPublicId}/users/{userPublicId}")
    @RolesAllowed({ "user-group:edit" })
    public Response removeUserFromGroup(@PathParam("groupPublicId") final String groupPublicId,
            @PathParam("userPublicId") final String userPublicId) {
        final boolean removed = this.groupService.removeUserFromGroup(userPublicId, groupPublicId);
        if (removed) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }
}
