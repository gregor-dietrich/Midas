package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.UserClient;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Component. */
@Timeout(value = 5, unit = ChronoUnit.SECONDS)
@ApplicationScoped
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class);

    @Inject
    @RestClient
    UserClient userClient;

    @Inject
    AuthService authService;

    /** Returns the all users. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<UserResponseDto> getAllUsers(final Integer page, final Integer size) {
        LOG.debug("Fetching all users");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.userClient.getAllUsers(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching users");
    }

    /** Returns the current user. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<UserResponseDto> getCurrentUser() {
        LOG.debug("Fetching current user");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.getCurrentUser(authHeader),
                this.authService, LOG, "fetching current user", response -> {
                    if (response.getStatus() == 200) {
                        return Optional
                                .of(ServiceHelper.readEntity(response, UserResponseDto.class, "current user response"));
                    } else if (response.getStatus() == 404) {
                        return Optional.empty();
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    /** Returns the user by id. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<UserResponseDto> getUserById(final Long id) {
        LOG.debugf("Fetching user with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.getUser(id, authHeader),
                this.authService, LOG, "fetching user " + id, response -> {
                    if (response.getStatus() == 200) {
                        return Optional.of(ServiceHelper.readEntity(response, UserResponseDto.class, "user response"));
                    } else if (response.getStatus() == 404) {
                        return Optional.empty();
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    /** Returns the user by username. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<UserResponseDto> getUserByUsername(final String username) {
        LOG.debugf("Fetching user with username: %s", username);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.getUserByUsername(username, authHeader),
                this.authService, LOG, "fetching user by username " + username, response -> {
                    if (response.getStatus() == 200) {
                        return Optional.of(ServiceHelper.readEntity(response, UserResponseDto.class, "user response"));
                    } else if (response.getStatus() == 404) {
                        return Optional.empty();
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    /** Creates a new user. */
    public UserResponseDto createUser(final UserDto user) {
        LOG.debugf("Creating new user: %s", user.getUsername());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.createUser(user, authHeader),
                this.authService, LOG, "creating user", "Failed to create user", response -> {
                    if (response.getStatus() == 201) {
                        return ServiceHelper.readEntity(response, UserResponseDto.class, "created user response");
                    }
                    throw ServiceHelper.responseFailure("Failed to create user", response);
                });
    }

    /** Updates the user. */
    public UserResponseDto updateUser(final UserDto user) {
        LOG.debugf("Updating user: %s", user.getId());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.updateUser(user.getId(), user, authHeader),
                this.authService, LOG, "updating user " + user.getId(), "Failed to update user", response -> {
                    if (response.getStatus() == 200) {
                        return ServiceHelper.readEntity(response, UserResponseDto.class, "updated user response");
                    }
                    throw ServiceHelper.responseFailure("Failed to update user", response);
                });
    }

    /** Updates the user password. */
    public UserResponseDto updateUserPassword(final UserDto user) {
        LOG.debugf("Updating password for user: %s", user.getId());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.patchUser(user.getId(), user, authHeader),
                this.authService, LOG, "updating user password " + user.getId(),
                "Failed to update user password", response -> {
                    if (response.getStatus() == 200) {
                        return ServiceHelper.readEntity(response, UserResponseDto.class,
                                "updated user password response");
                    }
                    throw ServiceHelper.responseFailure("Failed to update user password", response);
                });
    }

    /** Handles the delete user operation. */
    public boolean deleteUser(final Long id) {
        LOG.debugf("Deleting user: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.userClient.deleteUser(id, authHeader),
                this.authService, LOG, "deleting user " + id, response -> response.getStatus() == 204);
    }

    /** Handles the search users operation. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<UserResponseDto> searchUsers(final String query) {
        LOG.debugf("Searching users with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.userClient.searchUsers(query, authHeader),
                this.authService, LOG, "searching users");
    }
}
