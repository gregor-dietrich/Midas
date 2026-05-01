package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.UserGroupClient;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

/** Component. */
@Timeout(value = 5, unit = ChronoUnit.SECONDS)
@ApplicationScoped
public class UserGroupService {

    private static final Logger LOG = Logger.getLogger(UserGroupService.class);

    @Inject
    @RestClient
    UserGroupClient groupClient;

    @Inject
    AuthService authService;

    /** Returns the all groups. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<UserGroupResponseDto> getAllGroups(final Integer page, final Integer size) {
        LOG.debug("Fetching all groups");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.groupClient.getAllGroups(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching groups");
    }

    /** Returns the group by id. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<UserGroupResponseDto> getGroupById(final Long id) {
        LOG.debugf("Fetching group with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.groupClient.getGroup(id, authHeader),
                this.authService, LOG, "fetching group " + id, response -> {
                    if (response.getStatus() == 200) {
                        return Optional
                                .of(ServiceHelper.readEntity(response, UserGroupResponseDto.class, "group response"));
                    } else if (response.getStatus() == 404) {
                        return Optional.empty();
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    /** Returns the users in group. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<UserDto> getUsersInGroup(final Long groupId) {
        LOG.debugf("Fetching users in group: %s", groupId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.groupClient.getUsersInGroup(groupId, authHeader),
                this.authService, LOG, "fetching users in group " + groupId);
    }

    /** Handles the search groups operation. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<UserGroupResponseDto> searchGroups(final String query) {
        LOG.debugf("Searching groups with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.groupClient.searchGroups(query, authHeader),
                this.authService, LOG, "searching groups");
    }

    /** Creates a new group. */
    public UserGroupDto createGroup(final UserGroupDto group) {
        LOG.debugf("Creating new group: %s", group.getName());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.groupClient.createGroup(group, authHeader),
                this.authService, LOG, "creating group", "Failed to create group", response -> {
                    if (response.getStatus() == 201) {
                        final UserGroupResponseDto responseDto = ServiceHelper.readEntity(response,
                                UserGroupResponseDto.class, "created group response");
                        return responseDto.toUserGroupDto();
                    }
                    throw ServiceHelper.responseFailure("Failed to create group", response);
                });
    }

    /** Updates the group. */
    public UserGroupDto updateGroup(final UserGroupDto group) {
        LOG.debugf("Updating group: %s", group.getId());
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.groupClient.updateGroup(group.getId(), group, authHeader),
                this.authService, LOG, "updating group " + group.getId(), "Failed to update group", response -> {
                    if (response.getStatus() == 200) {
                        final UserGroupResponseDto responseDto = ServiceHelper.readEntity(response,
                                UserGroupResponseDto.class, "updated group response");
                        return responseDto.toUserGroupDto();
                    }
                    throw ServiceHelper.responseFailure("Failed to update group", response);
                });
    }

    /** Handles the delete group operation. */
    public boolean deleteGroup(final Long id) {
        LOG.debugf("Deleting group: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.groupClient.deleteGroup(id, authHeader),
                this.authService, LOG, "deleting group " + id, response -> response.getStatus() == 204);
    }

    /** Adds the user to group. */
    public boolean addUserToGroup(final Long groupId, final Long userId) {
        LOG.debugf("Adding user %s to group %s", userId, groupId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> {
            try {
                return this.groupClient.addUserToGroup(groupId, userId, authHeader);
            } catch (final WebApplicationException e) {
                if (e.getResponse().getStatus() == 409) {
                    throw new ServiceException("User is already a member of this group");
                }
                throw e;
            }
        }, this.authService, LOG, "adding user " + userId + " to group " + groupId,
                response -> response.getStatus() == 201);
    }

    /** Removes the user from group. */
    public boolean removeUserFromGroup(final Long groupId, final Long userId) {
        LOG.debugf("Removing user %s from group %s", userId, groupId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.groupClient.removeUserFromGroup(groupId, userId,
                authHeader), this.authService, LOG, "removing user " + userId + " from group " + groupId,
                response -> response.getStatus() == 204);
    }

    /** Returns the groups by user. */
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<UserGroupResponseDto> getGroupsByUser(final Long userId) {
        LOG.debugf("Fetching groups for user: %s", userId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.groupClient.getGroupsForUser(userId, authHeader),
                this.authService, LOG, "fetching groups for user " + userId);
    }
}
