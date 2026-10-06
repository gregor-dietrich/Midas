package de.vptr.midas.api.service;

import java.util.Set;

import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import de.vptr.midas.api.security.UserRankIdentityAugmentor;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Keeps a caller from acting above their own rank: they cannot modify or delete a user, or modify or delete a rank,
 * that holds a permission their own rank lacks, and cannot create or edit a rank, or give a user a rank, that grants
 * one. A rank is within the ceiling when its permissions are a subset of the caller's; an equal rank is.
 * <p>
 * {@link #of(String)} takes a snapshot of the caller's rank at the start of the change, inside the
 * {@link AdministratorGuard} lock on the paths that take it, so a change to the caller's own account or rank is judged
 * by the rank the caller held before it. The same snapshot decides which of a user's rank, banned and activated fields
 * the caller may set ({@link #holds(String)}). Calls without a caller, such as the bootstrap admin and tests, get
 * {@link #NONE}, which refuses nothing.
 */
public final class PrivilegeCeiling {

    /** No ceiling, for calls that do not act for an authenticated caller. */
    public static final PrivilegeCeiling NONE = new PrivilegeCeiling(null);

    /** Refusal message for a change to a user whose rank holds permissions the caller lacks. */
    public static final String MANAGE_USER_MESSAGE = "You cannot modify a user whose rank holds permissions you lack";

    /** Refusal message for a change to a rank that holds permissions the caller lacks. */
    public static final String MANAGE_RANK_MESSAGE = "You cannot modify a rank that holds permissions you lack";

    /** Refusal message for a rank, or a user's rank, that would grant permissions the caller lacks. */
    public static final String GRANT_MESSAGE = "You cannot grant permissions your own rank lacks";

    @Nullable
    private final Set<String> permissions;

    private PrivilegeCeiling(@Nullable final Set<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Reads the ceiling of the given caller from their rank. A caller without an account or rank holds no permission.
     *
     * @param currentUsername
     *            the authenticated caller, or null for a call that acts for no caller
     * @return the caller's ceiling, or {@link #NONE} without a caller
     */
    public static PrivilegeCeiling of(@Nullable final String currentUsername) {
        if (currentUsername == null) {
            return NONE;
        }
        final UserEntity caller = UserEntity.find("username", currentUsername).firstResult();
        return new PrivilegeCeiling(permissionsOf(caller != null ? caller.rank : null));
    }

    private static Set<String> permissionsOf(@Nullable final UserRankEntity rank) {
        return rank != null ? UserRankIdentityAugmentor.buildRolesFromUserRank(rank) : Set.of();
    }

    /** Tells whether the caller's rank holds the given role; a call without a caller holds every one. */
    public boolean holds(final String role) {
        return this.permissions == null || this.permissions.contains(role);
    }

    /** Refuses a change to, or the deletion of, a user with the given rank above the ceiling. */
    public void requireCanManageUser(@Nullable final UserRankEntity targetRank) {
        this.require(targetRank, MANAGE_USER_MESSAGE);
    }

    /** Refuses a change to a rank, as it is before the change, above the ceiling. */
    public void requireCanManageRank(@Nullable final UserRankEntity rank) {
        this.require(rank, MANAGE_RANK_MESSAGE);
    }

    /** Refuses a rank, as created or edited, or as given to a user, above the ceiling. */
    public void requireCanGrant(@Nullable final UserRankEntity rank) {
        this.require(rank, GRANT_MESSAGE);
    }

    private void require(@Nullable final UserRankEntity rank, final String message) {
        if (this.permissions != null && !this.permissions.containsAll(permissionsOf(rank))) {
            throw new WebApplicationException(message, Response.Status.FORBIDDEN);
        }
    }
}
