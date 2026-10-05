package de.vptr.midas.api.resource;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import de.vptr.midas.api.dto.PasswordChangeDto;
import de.vptr.midas.api.dto.UserDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.exception.ApiErrorResponse;
import de.vptr.midas.api.service.UserService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import de.vptr.midas.api.util.SecurityContextUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.Nullable;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/** REST resource for managing users. */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "User")
public class UserResource {

    @Inject
    UserService userService;

    @SuppressWarnings("UnnecessaryAssignment")
    void setUserServiceForTest(final UserService userService) {
        this.userService = userService;
    }

    @Context
    @Nullable
    SecurityContext securityContext;

    private String requireCurrentUsername() {
        return SecurityContextUtil.requireCurrentUsername(this.securityContext);
    }

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
    @APIResponseSchema(value = UserResponseDto.class, responseDescription = "Current user")
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getCurrentUser() {
        final String username = this.requireCurrentUsername();
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
    @APIResponse(responseCode = "200", description = "Password changed", content = {})
    @APIResponse(responseCode = "403", description = "User is banned or not activated",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Current password incorrect or new password invalid",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response changePassword(@Valid final PasswordChangeDto passwordChangeDto) {
        final String username = this.requireCurrentUsername();
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
    @APIResponseSchema(value = UserResponseDto.class, responseCode = "201", responseDescription = "User created")
    @APIResponse(responseCode = "403",
            description = "Missing role, or the new user's rank holds permissions the caller's rank lacks",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response createUser(@Valid final UserDto userDto) {
        stripProtectedFields(userDto);
        if (this.securityContext == null || !this.securityContext.isUserInRole("user-rank:edit")) {
            // The service gives the account the configured default rank
            userDto.rankPublicId = null;
        }
        final UserResponseDto created = this.userService.createUser(userDto, this.requireCurrentUsername());
        return ResponseUtil.created(created);
    }

    @GET
    @Path("/username/{username}")
    @RolesAllowed({ "user:delete", "user:edit" })
    @APIResponseSchema(value = UserResponseDto.class, responseDescription = "User found")
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Username longer than 255 characters",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getUserByUsername(@PathParam("username") @Size(max = 255) final String username) {
        return ResponseUtil.okOrNotFound(this.userService.findByUsername(username));
    }

    @GET
    @Path("/email")
    @RolesAllowed({ "user:delete", "user:edit" })
    @APIResponseSchema(value = UserResponseDto.class, responseDescription = "User found")
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Email longer than 255 characters",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getUserByEmail(@QueryParam("email") @Size(max = 255) final String email) {
        return ResponseUtil.okOrNotFound(this.userService.findByEmail(email));
    }

    /**
     * Get a user by publicId.
     * 
     * @param publicId
     *            the publicId
     * @return the user
     */
    @GET
    @Path("/{publicId}")
    @RolesAllowed({ "user:delete", "user:edit" })
    @APIResponseSchema(value = UserResponseDto.class, responseDescription = "User found")
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getUser(@PathParam("publicId") final String publicId) {
        return ResponseUtil.okOrNotFound(this.userService.findById(publicId));
    }

    /**
     * Update a user fully.
     * 
     * @param publicId
     *            the publicId
     * @param userDto
     *            the dto
     * @return true
     */
    @PUT
    @Path("/{publicId}")
    @RolesAllowed({ "user:edit" })
    @APIResponseSchema(value = UserResponseDto.class, responseDescription = "User replaced")
    @APIResponse(responseCode = "403",
            description = "Missing role, or the user's current or new rank holds permissions the caller's rank lacks",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response updateUser(@PathParam("publicId") final String publicId, @Valid final UserDto userDto) {
        stripProtectedFields(userDto);
        final UserResponseDto existing = this.userService.findById(publicId).orElse(null);
        enforceProtectedFieldPermissions(userDto, existing, this.securityContext);
        final UserResponseDto updated = this.userService.updateUser(publicId, userDto, this.requireCurrentUsername());
        return ResponseUtil.ok(updated);
    }

    /**
     * Patch a user.
     * 
     * @param publicId
     *            the user publicId
     * @param userDto
     *            patch data
     * @return the updated user
     */
    @PATCH
    @Path("/{publicId}")
    @RolesAllowed({ "user:edit" })
    @APIResponseSchema(value = UserResponseDto.class, responseDescription = "User updated")
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403",
            description = "Missing role, or the user's current or new rank holds permissions the caller's rank lacks",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response patchUser(@PathParam("publicId") final String publicId, final UserDto userDto) {
        stripProtectedFields(userDto);
        final UserResponseDto existing = this.userService.findById(publicId).orElse(null);
        enforceProtectedFieldPermissions(userDto, existing, this.securityContext);
        final UserResponseDto updated = this.userService.patchUser(publicId, userDto, this.requireCurrentUsername());
        return ResponseUtil.ok(updated);
    }

    /** Deletes the user with the given publicId. */
    @DELETE
    @Path("/{publicId}")
    @RolesAllowed({ "user:delete" })
    @APIResponse(responseCode = "204", description = "User deleted")
    @APIResponse(responseCode = "403",
            description = "Missing role, or the user's rank holds permissions the caller's rank lacks",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "User is the last active administrator",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response deleteUser(@PathParam("publicId") final String publicId) {
        final boolean deleted = this.userService.deleteUser(publicId, this.requireCurrentUsername());
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
        dto.lastIp = null;
    }

    /** Enforces role-based write protection for rank and account-status fields. */
    private static void enforceProtectedFieldPermissions(final UserDto dto, @Nullable final UserResponseDto existing,
            @Nullable final SecurityContext securityContext) {
        if (existing == null || securityContext == null) {
            return;
        }
        if (!securityContext.isUserInRole("user-rank:edit")) {
            dto.rankPublicId = existing.rankPublicId;
        }
        if (!securityContext.isUserInRole("admin:view")) {
            dto.banned = existing.banned;
            dto.activated = existing.activated;
        }
    }
}
