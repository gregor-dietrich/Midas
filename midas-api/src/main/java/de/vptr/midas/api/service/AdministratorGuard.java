package de.vptr.midas.api.service;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.vptr.midas.api.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;

/**
 * Refuses user and rank changes that would leave no active administrator.
 * <p>
 * An active administrator is a user with {@code activated = true} and {@code banned = false} whose rank grants every
 * permission. Under the {@link PrivilegeCeiling} only such a user can manage every user and rank and grant any
 * permission back, so only such a user can recover the system.
 * <p>
 * Every change that can remove one runs inside {@link #guard(Supplier)}, in the caller's transaction, which rolls back
 * on the refusal. Guarded transactions serialize on one transaction-scoped PostgreSQL advisory lock, so two of them
 * never interleave: the second waits for the first to commit, and both its counts then see the first one's change.
 * Logins and other row writes do not take that lock and are not blocked by it.
 */
@ApplicationScoped
public class AdministratorGuard {

    /** Refusal message for a change that would leave no active administrator. */
    public static final String LAST_ADMINISTRATOR_MESSAGE =
            "The last active administrator cannot be deleted, banned, deactivated or demoted";

    /** Every permission flag of {@code UserRankEntity}; a test keeps it in step with the entity. */
    static final List<String> PERMISSIONS = List.of("adminView", "pageAdd", "pageDelete", "pageEdit", "postAdd",
            "postDelete", "postEdit", "postCategoryAdd", "postCategoryDelete", "postCategoryEdit", "postCommentAdd",
            "postCommentDelete", "postCommentEdit", "userAdd", "userDelete", "userEdit", "userGroupAdd",
            "userGroupDelete", "userGroupEdit", "userAccountAdd", "userAccountDelete", "userAccountEdit", "userRankAdd",
            "userRankDelete", "userRankEdit");

    /** The class comment's definition as a JPQL condition on {@link UserEntity}. */
    static final String ACTIVE_ADMINISTRATOR = "activated = true AND banned = false"
            + PERMISSIONS.stream().map(flag -> " AND rank." + flag + " = true").collect(Collectors.joining());

    /**
     * Key of the advisory lock that guarded transactions serialize on, ASCII {@code MIDASADM}. Any constant works as
     * long as no other code takes an advisory lock with it; this is the only one, and PostgreSQL the only supported
     * database.
     */
    private static final long LOCK_KEY = 0x4D49_4441_5341_444DL;

    /**
     * Runs a change and refuses it if it removed the last active administrator. A system that had none before is left
     * alone, so that it can still be repaired.
     *
     * @param change
     *            the change, run once the lock is held
     * @param <T>
     *            the change's result type
     * @return the change's result
     * @throws ValidationException
     *             if there were active administrators before the change and none after it
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public <T> T guard(final Supplier<T> change) {
        UserEntity.getEntityManager().createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(?1)")
                .setParameter(1, LOCK_KEY).getSingleResult();
        // A new statement under READ COMMITTED sees every change committed before the lock was granted
        final long before = UserEntity.count(ACTIVE_ADMINISTRATOR);
        final T result = change.get();
        UserEntity.flush();
        if (before > 0 && UserEntity.count(ACTIVE_ADMINISTRATOR) == 0) {
            throw new ValidationException(LAST_ADMINISTRATOR_MESSAGE);
        }
        return result;
    }
}
