package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.UserClient;
import de.vptr.midas.gui.client.UserClient.DeleteUserRequest;
import de.vptr.midas.gui.client.UserClient.GetAllUsersRequest;
import de.vptr.midas.gui.client.UserClient.GetUserByUsernameRequest;
import de.vptr.midas.gui.client.UserClient.GetUserRequest;
import de.vptr.midas.gui.client.UserClient.PatchUserRequest;
import de.vptr.midas.gui.client.UserClient.SearchUsersRequest;
import de.vptr.midas.gui.client.UserClient.UpdateUserRequest;
import de.vptr.midas.gui.dto.PasswordChangeDto;
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
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.userClient.getAllUsers(GetAllUsersRequest.newInstance().page(safePage).size(safeSize)),
                this.authService, LOG, "fetching users");
    }

    /** Returns the current user, or empty if the backend does not find it. */
    public Optional<UserResponseDto> getCurrentUser() {
        LOG.debug("Fetching current user");
        return ServiceHelper.executeOptional(() -> this.userClient.getCurrentUser(), this.authService, LOG,
                "fetching current user");
    }

    /** Returns the user by id, or empty if it does not exist. */
    public Optional<UserResponseDto> getUserById(final String id) {
        LOG.debugf("Fetching user with ID: %s", id);
        return ServiceHelper.executeOptional(() -> this.userClient.getUser(GetUserRequest.newInstance().publicId(id)),
                this.authService, LOG, "fetching user " + id);
    }

    /** Returns the user by username, or empty if it does not exist. */
    public Optional<UserResponseDto> getUserByUsername(final String username) {
        LOG.debugf("Fetching user with username: %s", username);
        return ServiceHelper.executeOptional(
                () -> this.userClient.getUserByUsername(GetUserByUsernameRequest.newInstance().username(username)),
                this.authService, LOG, "fetching user by username " + username);
    }

    /** Creates a new user. */
    public UserResponseDto createUser(final UserDto user) {
        LOG.debugf("Creating new user: %s", user.getUsername());
        return ServiceHelper.execute(() -> this.userClient.createUser(user), this.authService, LOG, "creating user",
                "Failed to create user");
    }

    /** Updates the user. */
    public UserResponseDto updateUser(final UserDto user) {
        LOG.debugf("Updating user: %s", user.getPublicId());
        final var userId = Objects.requireNonNull(user.getPublicId(), "User ID must not be null for update");
        return ServiceHelper.execute(
                () -> this.userClient.updateUser(UpdateUserRequest.newInstance().publicId(userId), user),
                this.authService, LOG, "updating user " + user.getPublicId(), "Failed to update user");
    }

    /** Updates the user password. */
    public UserResponseDto updateUserPassword(final UserDto user) {
        LOG.debugf("Updating password for user: %s", user.getPublicId());
        final var userId = Objects.requireNonNull(user.getPublicId(), "User ID must not be null for password update");
        return ServiceHelper.execute(
                () -> this.userClient.patchUser(PatchUserRequest.newInstance().publicId(userId), user),
                this.authService, LOG, "updating user password " + user.getPublicId(),
                "Failed to update user password");
    }

    /** Handles the delete user operation. */
    public void deleteUser(final String id) {
        LOG.debugf("Deleting user: %s", id);
        ServiceHelper.executeVoid(() -> this.userClient.deleteUser(DeleteUserRequest.newInstance().publicId(id)),
                this.authService, LOG, "deleting user " + id);
    }

    /** Handles the search users operation. */
    public List<UserResponseDto> searchUsers(final String query) {
        LOG.debugf("Searching users with query: %s", query);
        return ServiceHelper.execute(() -> this.userClient.searchUsers(SearchUsersRequest.newInstance().query(query)),
                this.authService, LOG, "searching users");
    }

    /** Changes the password for the current user. */
    public void changePassword(final PasswordChangeDto passwordChangeDto) {
        LOG.debug("Changing password for current user");
        ServiceHelper.executeVoid(() -> this.userClient.changePassword(passwordChangeDto), this.authService, LOG,
                "changing password");
    }
}
