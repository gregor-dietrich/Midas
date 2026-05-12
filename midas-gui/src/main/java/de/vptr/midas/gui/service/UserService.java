package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.UserClient;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Service for user operations. */
@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    @Inject
    @RestClient
    UserClient userClient;

    @Inject
    AuthService authService;

    /** Returns the all users. */
    public List<UserResponseDto> getAllUsers(final Integer page, final Integer size) {
        LOG.debug("Fetching all users");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.userClient.getAllUsers(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching users");
    }

    /** Returns the current user. */
    public Optional<UserResponseDto> getCurrentUser() {
        LOG.debug("Fetching current user");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.getCurrentUser(authHeader), this.authService,
                LOG, "fetching current user",
                response -> ServiceHelper.readOptionalEntity(response, UserResponseDto.class, "current user response"));
    }

    /** Returns the user by id. */
    public Optional<UserResponseDto> getUserById(final Long id) {
        LOG.debugf("Fetching user with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.getUser(id, authHeader), this.authService, LOG,
                "fetching user " + id,
                response -> ServiceHelper.readOptionalEntity(response, UserResponseDto.class, "user response"));
    }

    /** Returns the user by username. */
    public Optional<UserResponseDto> getUserByUsername(final String username) {
        LOG.debugf("Fetching user with username: %s", username);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.getUserByUsername(username, authHeader),
                this.authService, LOG, "fetching user by username " + username,
                response -> ServiceHelper.readOptionalEntity(response, UserResponseDto.class, "user response"));
    }

    /** Creates a new user. */
    public UserResponseDto createUser(final UserDto user) {
        LOG.debugf("Creating new user: %s", user.getUsername());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.createUser(user, authHeader), this.authService,
                LOG, "creating user", "Failed to create user",
                response -> ServiceHelper.readExpectedStatusEntity(response, 201, UserResponseDto.class,
                        "created user response", "Failed to create user"));
    }

    /** Updates the user. */
    public UserResponseDto updateUser(final UserDto user) {
        LOG.debugf("Updating user: %s", user.getId());
        final var authHeader = this.authService.requireAuthorizationHeader();
        final var userId = Objects.requireNonNull(user.getId(), "User ID must not be null for update");
        return ServiceHelper.executeWithResponse(() -> this.userClient.updateUser(userId, user, authHeader),
                this.authService, LOG, "updating user " + user.getId(), "Failed to update user",
                response -> ServiceHelper.readExpectedStatusEntity(response, 200, UserResponseDto.class,
                        "updated user response", "Failed to update user"));
    }

    /** Updates the user password. */
    public UserResponseDto updateUserPassword(final UserDto user) {
        LOG.debugf("Updating password for user: %s", user.getId());
        final var authHeader = this.authService.requireAuthorizationHeader();
        final var userId = Objects.requireNonNull(user.getId(), "User ID must not be null for password update");
        return ServiceHelper.executeWithResponse(() -> this.userClient.patchUser(userId, user, authHeader),
                this.authService, LOG, "updating user password " + user.getId(), "Failed to update user password",
                response -> ServiceHelper.readExpectedStatusEntity(response, 200, UserResponseDto.class,
                        "updated user password response", "Failed to update user password"));
    }

    /** Handles the delete user operation. */
    public boolean deleteUser(final Long id) {
        LOG.debugf("Deleting user: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.deleteUser(id, authHeader), this.authService,
                LOG, "deleting user " + id, response -> response.getStatus() == 204);
    }

    /** Handles the search users operation. */
    public List<UserResponseDto> searchUsers(final String query) {
        LOG.debugf("Searching users with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.userClient.searchUsers(query, authHeader), this.authService, LOG,
                "searching users");
    }
}
