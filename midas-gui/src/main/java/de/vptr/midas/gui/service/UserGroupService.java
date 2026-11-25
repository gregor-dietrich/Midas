package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.UserGroupClient;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.LogSanitizer;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;

/** Service for user group operations. */
@ApplicationScoped
public class UserGroupService {

    private static final Logger LOG = Logger.getLogger(UserGroupService.class);

    @Inject
    @RestClient
    UserGroupClient groupClient;

    @Inject
    AuthService authService;

    /** Returns the all groups. */
    public List<UserGroupResponseDto> getAllGroups(final Integer page, final Integer size) {
        LOG.debug("Fetching all groups");
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.groupClient.getAllGroups(safePage, safeSize), this.authService, LOG,
                "fetching groups");
    }

    /** Returns the group by publicId, or empty if it does not exist. */
    public Optional<UserGroupResponseDto> getGroupById(final String publicId) {
        LOG.debugf("Fetching group with publicID: %s", LogSanitizer.sanitize(publicId));
        return ServiceHelper.executeOptional(() -> this.groupClient.getGroup(publicId), this.authService, LOG,
                "fetching group " + publicId);
    }

    /** Returns the users in group. */
    public List<UserDto> getUsersInGroup(final String groupPublicId) {
        LOG.debugf("Fetching users in group: %s", LogSanitizer.sanitize(groupPublicId));
        return ServiceHelper.execute(() -> this.groupClient.getUsersInGroup(groupPublicId), this.authService, LOG,
                "fetching users in group " + groupPublicId).stream().map(UserDto::from).toList();
    }

    /** Handles the search groups operation. */
    public List<UserGroupResponseDto> searchGroups(final String query) {
        LOG.debugf("Searching groups with query: %s", LogSanitizer.sanitize(query));
        return ServiceHelper.execute(() -> this.groupClient.searchGroups(query), this.authService, LOG,
                "searching groups");
    }

    /** Creates a new group. */
    public UserGroupDto createGroup(final UserGroupDto group) {
        LOG.debugf("Creating new group: %s", LogSanitizer.sanitize(group.getName()));
        return UserGroupDto.from(ServiceHelper.execute(() -> this.groupClient.createGroup(group), this.authService, LOG,
                "creating group", "Failed to create group"));
    }

    /** Updates the group. */
    public UserGroupDto updateGroup(final UserGroupDto group) {
        final String publicId = Objects.requireNonNull(group.getPublicId(), "group.publicId");
        LOG.debugf("Updating group: %s", LogSanitizer.sanitize(publicId));
        return UserGroupDto.from(ServiceHelper.execute(() -> this.groupClient.updateGroup(publicId, group),
                this.authService, LOG, "updating group " + publicId, "Failed to update group"));
    }

    /** Handles the delete group operation. */
    public void deleteGroup(final String publicId) {
        LOG.debugf("Deleting group: %s", LogSanitizer.sanitize(publicId));
        ServiceHelper.executeVoid(() -> this.groupClient.deleteGroup(publicId), this.authService, LOG,
                "deleting group " + publicId);
    }

    /** Adds the user to group. */
    public void addUserToGroup(final String groupPublicId, final String userPublicId) {
        LOG.debugf("Adding user %s to group %s", LogSanitizer.sanitize(userPublicId),
                LogSanitizer.sanitize(groupPublicId));
        ServiceHelper.executeVoid(() -> {
            try {
                this.groupClient.addUserToGroup(groupPublicId, userPublicId);
            } catch (final WebApplicationException e) {
                if (e.getResponse().getStatus() == 409) {
                    e.getResponse().close();
                    throw new ServiceException("User is already a member of this group", e);
                }
                throw e;
            }
        }, this.authService, LOG, "adding user " + userPublicId + " to group " + groupPublicId);
    }

    /** Removes the user from group. */
    public void removeUserFromGroup(final String groupPublicId, final String userPublicId) {
        LOG.debugf("Removing user %s from group %s", LogSanitizer.sanitize(userPublicId),
                LogSanitizer.sanitize(groupPublicId));
        ServiceHelper.executeVoid(() -> this.groupClient.removeUserFromGroup(groupPublicId, userPublicId),
                this.authService, LOG, "removing user " + userPublicId + " from group " + groupPublicId);
    }

    /** Returns the groups by user. */
    public List<UserGroupResponseDto> getGroupsByUser(final String userPublicId) {
        LOG.debugf("Fetching groups for user: %s", LogSanitizer.sanitize(userPublicId));
        return ServiceHelper.execute(() -> this.groupClient.getGroupsForUser(userPublicId), this.authService, LOG,
                "fetching groups for user " + userPublicId);
    }
}
