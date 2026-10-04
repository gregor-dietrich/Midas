package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.UserGroupClient;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

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
        LOG.debugf("Fetching group with publicID: %s", publicId);
        return ServiceHelper.executeOptional(() -> this.groupClient.getGroup(publicId), this.authService, LOG,
                "fetching group " + publicId);
    }

    /** Returns the users in group. */
    public List<UserDto> getUsersInGroup(final String groupPublicId) {
        LOG.debugf("Fetching users in group: %s", groupPublicId);
        return ServiceHelper.execute(() -> this.groupClient.getUsersInGroup(groupPublicId), this.authService, LOG,
                "fetching users in group " + groupPublicId).stream().map(UserDto::from).toList();
    }

    /** Handles the search groups operation. */
    public List<UserGroupResponseDto> searchGroups(final String query) {
        LOG.debugf("Searching groups with query: %s", query);
        return ServiceHelper.execute(() -> this.groupClient.searchGroups(query), this.authService, LOG,
                "searching groups");
    }

    /** Creates a new group. */
    public UserGroupDto createGroup(final UserGroupDto group) {
        LOG.debugf("Creating new group: %s", group.getName());
        return UserGroupDto.from(ServiceHelper.execute(() -> this.groupClient.createGroup(group), this.authService, LOG,
                "creating group", "Failed to create group"));
    }

    /** Updates the group. */
    public UserGroupDto updateGroup(final UserGroupDto group) {
        LOG.debugf("Updating group: %s", group.getPublicId());
        return UserGroupDto.from(ServiceHelper.execute(
                () -> this.groupClient.updateGroup(Objects.requireNonNull(group.getPublicId()), group),
                this.authService, LOG, "updating group " + group.getPublicId(), "Failed to update group"));
    }

    /** Handles the delete group operation. */
    public void deleteGroup(final String publicId) {
        LOG.debugf("Deleting group: %s", publicId);
        ServiceHelper.executeVoid(() -> this.groupClient.deleteGroup(publicId), this.authService, LOG,
                "deleting group " + publicId);
    }

    /** Adds the user to group. */
    public void addUserToGroup(final String groupPublicId, final String userPublicId) {
        LOG.debugf("Adding user %s to group %s", userPublicId, groupPublicId);
        ServiceHelper.executeVoid(() -> {
            try {
                this.groupClient.addUserToGroup(groupPublicId, userPublicId);
            } catch (final WebApplicationException e) {
                if (e.getResponse().getStatus() == 409) {
                    e.getResponse().close();
                    throw new ServiceException("User is already a member of this group");
                }
                throw e;
            }
        }, this.authService, LOG, "adding user " + userPublicId + " to group " + groupPublicId);
    }

    /** Removes the user from group. */
    public void removeUserFromGroup(final String groupPublicId, final String userPublicId) {
        LOG.debugf("Removing user %s from group %s", userPublicId, groupPublicId);
        ServiceHelper.executeVoid(() -> this.groupClient.removeUserFromGroup(groupPublicId, userPublicId),
                this.authService, LOG, "removing user " + userPublicId + " from group " + groupPublicId);
    }

    /** Returns the groups by user. */
    public List<UserGroupResponseDto> getGroupsByUser(final String userPublicId) {
        LOG.debugf("Fetching groups for user: %s", userPublicId);
        return ServiceHelper.execute(() -> this.groupClient.getGroupsForUser(userPublicId), this.authService, LOG,
                "fetching groups for user " + userPublicId);
    }
}
