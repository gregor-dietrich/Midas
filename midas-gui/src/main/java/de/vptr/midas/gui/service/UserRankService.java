package de.vptr.midas.gui.service;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.client.UserRankClient;
import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/** Service for user rank operations. */
@ApplicationScoped
public class UserRankService {

    private static final Logger LOG = Logger.getLogger(UserRankService.class);
    private static final SessionAttribute<UserRankResponseDto> CURRENT_USER_RANK_ATTRIBUTE =
            SessionAttribute.of("current.user.rank", UserRankResponseDto.class);
    private static final SessionAttribute<Long> CURRENT_USER_RANK_TIMESTAMP_ATTRIBUTE =
            SessionAttribute.of("current.user.rank.timestamp", Long.class);

    private static final int CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    @Inject
    @RestClient
    UserRankClient userRankClient;

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    /** Returns the all ranks. */
    public List<UserRankResponseDto> getAllRanks() {
        LOG.debug("Fetching all ranks");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.userRankClient.getAllRanks(authHeader), this.authService, LOG,
                "fetching ranks");
    }

    /** Returns the current user rank. */
    public UserRankResponseDto getCurrentUserRank() {
        final var emptyRank = new UserRankResponseDto();

        // Check session cache first to avoid repeated REST calls
        final var session = VaadinSession.getCurrent();
        if (session != null) {
            final var cachedRank = CURRENT_USER_RANK_ATTRIBUTE.read(session, LOG).orElse(null);
            final var lastCacheTime = CURRENT_USER_RANK_TIMESTAMP_ATTRIBUTE.read(session, LOG).orElse(null);
            final var currentTime = System.currentTimeMillis();

            if (cachedRank != null && lastCacheTime != null && (currentTime - lastCacheTime) < CACHE_VALIDITY_MS) {
                LOG.debugf("Using cached user rank data (%sms old)", currentTime - lastCacheTime);
                return cachedRank;
            }
            if (cachedRank != null || lastCacheTime != null) {
                CURRENT_USER_RANK_ATTRIBUTE.clear(session);
                CURRENT_USER_RANK_TIMESTAMP_ATTRIBUTE.clear(session);
            }
        }

        LOG.debug("Fetching fresh user rank data");
        final var currentUser = this.userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            LOG.warn("Could not get current user for permission check");
            return emptyRank;
        }

        final var user = currentUser.get();
        if (user.getRankPublicId() == null) {
            LOG.warn("User has no rank assigned for permission check");
            return emptyRank;
        }

        try {
            final var userRank = this.getRankById(user.getRankPublicId());

            // Cache the result in session
            if (session != null) {
                CURRENT_USER_RANK_ATTRIBUTE.write(session, userRank);
                CURRENT_USER_RANK_TIMESTAMP_ATTRIBUTE.write(session, System.currentTimeMillis());
                LOG.debug("Cached user rank data");
            }

            return userRank;
        } catch (final AuthenticationException | ServiceException e) {
            LOG.errorf(e, "Error fetching user rank for permission check: %s", e.getMessage());
            return emptyRank;
        }
    }

    /**
     * Clear the cached user rank from session. Call this when user data might have changed.
     */
    public void clearCachedUserRank() {
        final var session = VaadinSession.getCurrent();
        if (session != null) {
            CURRENT_USER_RANK_ATTRIBUTE.clear(session);
            CURRENT_USER_RANK_TIMESTAMP_ATTRIBUTE.clear(session);
            LOG.trace("Cleared cached user rank");
        }
    }

    /** Returns the rank by id. */
    public UserRankResponseDto getRankById(final String id) {
        LOG.infof("Getting user rank by id: %s", id);
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.userRankClient.getRank(id, authHeader);
        }, this.authService, LOG, "getting user rank " + id, response -> {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                final var rank = ServiceHelper.readEntity(response, UserRankResponseDto.class, "user rank response");
                LOG.infof("Successfully retrieved user rank: %s", rank.getName());
                return rank;
            } else {
                throw new ServiceException("User rank not found");
            }
        });
    }

    /** Creates a new rank. */
    public UserRankDto createRank(final UserRankDto rank) {
        LOG.infof("Creating user rank: %s", rank.getName());
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.userRankClient.createRank(rank, authHeader);
        }, this.authService, LOG, "creating user rank", "Failed to create user rank", response -> {
            if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                final var createdRank = ServiceHelper.readEntity(response, UserRankDto.class, "created user rank");
                LOG.infof("Successfully created user rank: %s", createdRank.getName());
                return createdRank;
            }
            throw ServiceHelper.responseFailure("Failed to create user rank", response);
        });
    }

    /** Updates the rank. */
    public UserRankDto updateRank(final UserRankDto rank) {
        LOG.infof("Updating user rank: %s (%s)", rank.getName(), rank.getPublicId());
        return ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.userRankClient.updateRank(rank.getPublicId(), rank, authHeader);
        }, this.authService, LOG, "updating user rank " + rank.getPublicId(), "Failed to update user rank",
                response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        final var updatedRank =
                                ServiceHelper.readEntity(response, UserRankDto.class, "updated user rank");
                        LOG.infof("Successfully updated user rank: %s", updatedRank.getName());
                        return updatedRank;
                    }
                    throw ServiceHelper.responseFailure("Failed to update user rank", response);
                });
    }

    /** Handles the search ranks operation. */
    public List<UserRankResponseDto> searchRanks(final String query) {
        LOG.debugf("Searching ranks with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.userRankClient.searchRanks(query, authHeader), this.authService, LOG,
                "searching ranks");
    }

    /** Handles the delete rank operation. */
    public void deleteRank(final String id) {
        LOG.infof("Deleting user rank: %s", id);
        ServiceHelper.executeWithResponse(() -> {
            final var authHeader = this.authService.requireAuthorizationHeader();
            return this.userRankClient.deleteRank(id, authHeader);
        }, this.authService, LOG, "deleting user rank " + id, response -> {
            if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                LOG.infof("Successfully deleted user rank: %s", id);
                return null;
            } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ServiceException("User rank not found");
            }
            throw ServiceHelper.responseFailure("Failed to delete user rank", response);
        });
    }

}
