package de.vptr.midas.gui.service;

import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.client.UserRankClient;
import de.vptr.midas.gui.client.UserRankClient.DeleteRankRequest;
import de.vptr.midas.gui.client.UserRankClient.GetRankRequest;
import de.vptr.midas.gui.client.UserRankClient.SearchRanksRequest;
import de.vptr.midas.gui.client.UserRankClient.UpdateRankRequest;
import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Service for user rank operations. */
@ApplicationScoped
public class UserRankService {

    private static final Logger LOG = Logger.getLogger(UserRankService.class);
    private static final SessionAttribute<UserRankDto> CURRENT_USER_RANK_ATTRIBUTE =
            SessionAttribute.of("current.user.rank", UserRankDto.class);
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
        return ServiceHelper.execute(() -> this.userRankClient.getAllRanks(), this.authService, LOG, "fetching ranks");
    }

    /** Returns the current user rank. */
    public UserRankDto getCurrentUserRank() {
        final var emptyRank = new UserRankDto();

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
            final var userRank = UserRankDto.from(this.getRankById(user.getRankPublicId()));

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
        final var rank = ServiceHelper
                .executeOptional(() -> this.userRankClient.getRank(GetRankRequest.newInstance().publicId(id)),
                        this.authService, LOG, "getting user rank " + id)
                .orElseThrow(() -> new ServiceException("User rank not found"));
        LOG.infof("Successfully retrieved user rank: %s", rank.getName());
        return rank;
    }

    /** Creates a new rank. */
    public UserRankDto createRank(final UserRankDto rank) {
        LOG.infof("Creating user rank: %s", rank.getName());
        final var createdRank = UserRankDto.from(ServiceHelper.execute(() -> this.userRankClient.createRank(rank),
                this.authService, LOG, "creating user rank", "Failed to create user rank"));
        LOG.infof("Successfully created user rank: %s", createdRank.getName());
        return createdRank;
    }

    /** Updates the rank. */
    public UserRankDto updateRank(final UserRankDto rank) {
        final String publicId = Objects.requireNonNull(rank.getPublicId(), "rank.publicId");
        LOG.infof("Updating user rank: %s (%s)", rank.getName(), publicId);
        final var updatedRank = UserRankDto.from(ServiceHelper.execute(
                () -> this.userRankClient.updateRank(UpdateRankRequest.newInstance().publicId(publicId), rank),
                this.authService, LOG, "updating user rank " + publicId, "Failed to update user rank"));
        LOG.infof("Successfully updated user rank: %s", updatedRank.getName());
        return updatedRank;
    }

    /** Handles the search ranks operation. */
    public List<UserRankResponseDto> searchRanks(final String query) {
        LOG.debugf("Searching ranks with query: %s", query);
        return ServiceHelper.execute(
                () -> this.userRankClient.searchRanks(SearchRanksRequest.newInstance().query(query)), this.authService,
                LOG, "searching ranks");
    }

    /** Handles the delete rank operation. */
    public void deleteRank(final String id) {
        LOG.infof("Deleting user rank: %s", id);
        ServiceHelper.executeVoid(() -> this.userRankClient.deleteRank(DeleteRankRequest.newInstance().publicId(id)),
                this.authService, LOG, "deleting user rank " + id, "User rank not found");
        LOG.infof("Successfully deleted user rank: %s", id);
    }
}
