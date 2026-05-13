package de.vptr.midas.api.resource;

import java.util.List;

import de.vptr.midas.api.dto.PasswordChangeDto;
import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.service.UserService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import de.vptr.midas.api.util.SecurityContextUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/** REST resource for managing users. */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class UserResource {

    /** Default rank ID assigned to newly registered users ("User" rank). */
    private static final long DEFAULT_USER_RANK_ID = 2L;

    @Inject
    UserService userService;

    @Context
    @Nullable
    SecurityContext securityContext;

    /** Returns all users with normalized pagination parameters. */
    @GET
    @RolesAllowed({ "user:delete", "user:edit" })
    public List<UserResponseDto> getAllUsers(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.userService.getAllUsers(safePage, safeSize);
    }

    /**
     * Get the currently logged-in user.
     * 
     * @return current user
     */
    @GET
    @Path("/me")
    public Response getCurrentUser() {
        final String username = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        return ResponseUtil.okOrNotFound(this.userService.findByUsername(username));
    }

    /**
     * Change password for the currently logged-in user.
     * 
     * @param passwordChangeDto
     *            the password change data
     * @return ok
     */
    @POST
    @Path("/me/password")
    public Response changePassword(@Valid final PasswordChangeDto passwordChangeDto) {
        final String username = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        this.userService.changePassword(username, passwordChangeDto);
        return ResponseUtil.ok();
    }

    /**
     * Create a user.
     * 
     * @param userDto
     *            the user data
     * @return the created user
     */
    @POST
    @RolesAllowed({ "user:add" })
    public Response createUser(@Valid final UserDto userDto) {
        stripProtectedFields(userDto);
        if (userDto.rankId != null && userDto.rankId != DEFAULT_USER_RANK_ID
                && (this.securityContext == null || !this.securityContext.isUserInRole("user-rank:edit"))) {
            userDto.rankId = DEFAULT_USER_RANK_ID;
        }
        final UserResponseDto created = this.userService.createUser(userDto);
        return ResponseUtil.created(created);
    }

    @GET
    @Path("/username/{username}")
    @RolesAllowed({ "user:delete", "user:edit" })
    public Response getUserByUsername(@PathParam("username") @Size(max = 255) final String username) {
        return ResponseUtil.okOrNotFound(this.userService.findByUsername(username));
    }

    @GET
    @Path("/email")
    @RolesAllowed({ "user:delete", "user:edit" })
    public Response getUserByEmail(@QueryParam("email") @Size(max = 255) final String email) {
        return ResponseUtil.okOrNotFound(this.userService.findByEmail(email));
    }

    /**
     * Get a user by ID.
     * 
     * @param id
     *            the id
     * @return the user
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({ "user:delete", "user:edit" })
    public Response getUser(@PathParam("id") @Min(1) final Long id) {
        return ResponseUtil.okOrNotFound(this.userService.findById(id));
    }

    /**
     * Update a user fully.
     * 
     * @param id
     *            the id
     * @param userDto
     *            the dto
     * @return true
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ "user:edit" })
    public Response updateUser(@PathParam("id") @Min(1) final Long id, @Valid final UserDto userDto) {
        stripProtectedFields(userDto);
        final UserResponseDto existing = this.userService.findById(id).orElse(null);
        enforceProtectedFieldPermissions(userDto, existing, this.securityContext);
        final UserResponseDto updated = this.userService.updateUser(id, userDto);
        return ResponseUtil.ok(updated);
    }

    /**
     * Patch a user.
     * 
     * @param id
     *            the user id
     * @param userDto
     *            patch data
     * @return the updated user
     */
    @PATCH
    @Path("/{id}")
    @RolesAllowed({ "user:edit" })
    public Response patchUser(@PathParam("id") @Min(1) final Long id, final UserDto userDto) {
        stripProtectedFields(userDto);
        final UserResponseDto existing = this.userService.findById(id).orElse(null);
        enforceProtectedFieldPermissions(userDto, existing, this.securityContext);
        final UserResponseDto updated = this.userService.patchUser(id, userDto);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the user with the given id. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "user:delete" })
    public Response deleteUser(@PathParam("id") @Min(1) final Long id) {
        final boolean deleted = this.userService.deleteUser(id);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    @GET
    @Path("/search")
    @RolesAllowed({ "user:delete", "user:edit" })
    public List<UserResponseDto> searchUsers(@QueryParam("query") final String query) {
        return this.userService.searchUsers(query);
    }

    /** Strips fields that should never be client-settable from the DTO. */
    private static void stripProtectedFields(final UserDto dto) {
        dto.activationKey = null;
        dto.lastIp = null;
    }

    /** Enforces role-based write protection for rank and account-status fields. */
    private static void enforceProtectedFieldPermissions(final UserDto dto, @Nullable final UserResponseDto existing,
            @Nullable final SecurityContext securityContext) {
        if (existing == null || securityContext == null) {
            return;
        }
        if (!securityContext.isUserInRole("user-rank:edit")) {
            dto.rankId = existing.rankId;
        }
        if (!securityContext.isUserInRole("admin:view")) {
            dto.banned = existing.banned;
            dto.activated = existing.activated;
        }
    }
}
