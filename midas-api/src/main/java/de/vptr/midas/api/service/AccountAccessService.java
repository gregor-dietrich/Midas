package de.vptr.midas.api.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.vptr.midas.api.dto.AccountResponseDto;
import de.vptr.midas.api.dto.PaymentResponseDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.PaymentEntity;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserAccountMetaEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.PerformanceMonitor;
import de.vptr.midas.api.util.QueryAggregationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for account access, user-account linking, and account queries. */
@ApplicationScoped
public class AccountAccessService {

    private static final int MAX_ACCOUNTS_LIMIT = 100;

    @Inject
    AccountResponseAssembler accountResponseAssembler;

    /** Returns all users associated with the given account. */
    public List<UserResponseDto> getAssociatedUsers(final Long accountId) {
        final AccountEntity account = AccountEntity.findById(accountId);
        if (account == null) {
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }
        final List<UserEntity> users = UserEntity.find("""
                SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.id IN \
                (SELECT m.user.id FROM UserAccountMetaEntity m WHERE m.account.id = ?1)""", accountId).list();
        if (users.isEmpty()) {
            return List.of();
        }
        final List<Long> userIds = users.stream().map(u -> u.id).toList();

        final List<Object[]> postResults = PostEntity.getEntityManager()
                .createQuery("SELECT p.user.id, COUNT(p) FROM PostEntity p WHERE p.user.id IN :ids GROUP BY p.user.id",
                        Object[].class)
                .setParameter("ids", userIds).getResultList();
        final Map<Long, Long> postCounts = QueryAggregationUtil.toLongCountMap(postResults);

        final List<Object[]> commentResults = PostCommentEntity.getEntityManager().createQuery(
                "SELECT c.user.id, COUNT(c) FROM PostCommentEntity c WHERE c.user.id IN :ids GROUP BY c.user.id",
                Object[].class).setParameter("ids", userIds).getResultList();
        final Map<Long, Long> commentCounts = QueryAggregationUtil.toLongCountMap(commentResults);

        final List<UserAccountMetaEntity> metas = UserAccountMetaEntity.findByAccountId(accountId);
        final Map<Long, Boolean> isNegativeByUser = new HashMap<>();
        for (final UserAccountMetaEntity m : metas) {
            final UserEntity u = m.user;
            if (u != null && u.id != null) {
                isNegativeByUser.put(u.id, m.isNegative);
            }
        }

        return users.stream().map(u -> {
            final UserResponseDto dto =
                    new UserResponseDto(u, postCounts.getOrDefault(u.id, 0L), commentCounts.getOrDefault(u.id, 0L));
            dto.isNegative = isNegativeByUser.getOrDefault(u.id, false);
            return dto;
        }).toList();
    }

    /** Returns all accounts for the given user. */
    public List<AccountResponseDto> getAccountsForUser(final Long userId) {
        return PerformanceMonitor.time("getAccountsForUser(" + userId + ")", () -> {
            final UserEntity user = UserEntity.findById(userId);
            if (user == null) {
                throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
            }

            final List<AccountEntity> accounts = AccountEntity.find("""
                    SELECT a FROM AccountEntity a WHERE a.id IN \
                    (SELECT m.account.id FROM UserAccountMetaEntity m WHERE m.user.id = ?1)""", userId).list();
            return this.accountResponseAssembler.toAccountResponseDtos(accounts);
        });
    }

    /** Returns all payments for the given account. */
    public List<PaymentResponseDto> getAccountPayments(final Long accountId) {
        return PerformanceMonitor.time("getAccountPayments(" + accountId + ")", () -> {
            @SuppressWarnings("unchecked")
            final List<PaymentEntity> payments = PaymentEntity.getEntityManager().createQuery("""
                    SELECT p FROM PaymentEntity p \
                    LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user \
                    WHERE p.sourceAccount.id = :accountId OR p.targetAccount.id = :accountId \
                    ORDER BY p.date DESC""").setParameter("accountId", accountId).getResultList();

            return payments.stream().map(PaymentResponseDto::new).toList();
        });
    }

    private static final int MAX_PAYMENTS = 1_000;

    /** Returns all outgoing payments for the given account. */
    public List<PaymentResponseDto> getOutgoingPayments(final Long accountId) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.sourceAccount.id = ?1",
                accountId).page(0, MAX_PAYMENTS).list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns all incoming payments for the given account. */
    public List<PaymentResponseDto> getIncomingPayments(final Long accountId) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.targetAccount.id = ?1",
                accountId).page(0, MAX_PAYMENTS).list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns the current balance for the given account. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public BigDecimal getAccountBalance(final Long accountId) {
        return PerformanceMonitor.time("getAccountBalance(" + accountId + ")",
                () -> PaymentEntity.getEntityManager().createQuery("""
                        SELECT \
                        COALESCE(SUM(CASE WHEN p.targetAccount.id = :accountId THEN p.amount ELSE 0 END), 0) - \
                        COALESCE(SUM(CASE WHEN p.sourceAccount.id = :accountId THEN p.amount ELSE 0 END), 0) \
                        FROM PaymentEntity p \
                        WHERE p.targetAccount.id = :accountId OR p.sourceAccount.id = :accountId""", BigDecimal.class)
                        .setParameter("accountId", accountId).getSingleResult());
    }

    /** Returns balances for the given list of account ids. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<Long, BigDecimal> getAccountBalances(final List<Long> accountIds) {
        return PerformanceMonitor.time("getAccountBalances(" + accountIds.size() + " accounts)", () -> {
            if (accountIds == null || accountIds.isEmpty()) {
                return Map.of();
            }

            final Map<Long, BigDecimal> balances = new HashMap<>();
            accountIds.forEach(id -> balances.put(id, BigDecimal.ZERO));

            // Compute incoming payments (target) aggregated by account
            final List<Object[]> incomingResults = PaymentEntity.getEntityManager().createQuery("""
                    SELECT p.targetAccount.id, COALESCE(SUM(p.amount), 0) FROM PaymentEntity p \
                    WHERE p.targetAccount.id IN :accountIds GROUP BY p.targetAccount.id""", Object[].class)
                    .setParameter("accountIds", accountIds).getResultList();

            for (final Object[] row : incomingResults) {
                final Long accountId = (Long) row[0];
                final BigDecimal amount = (BigDecimal) row[1];
                if (accountId != null) {
                    balances.put(accountId, balances.getOrDefault(accountId, BigDecimal.ZERO).add(amount));
                }
            }

            // Compute outgoing payments (source) aggregated by account
            final List<Object[]> outgoingResults = PaymentEntity.getEntityManager().createQuery("""
                    SELECT p.sourceAccount.id, COALESCE(SUM(p.amount), 0) FROM PaymentEntity p \
                    WHERE p.sourceAccount.id IN :accountIds GROUP BY p.sourceAccount.id""", Object[].class)
                    .setParameter("accountIds", accountIds).getResultList();

            for (final Object[] row : outgoingResults) {
                final Long accountId = (Long) row[0];
                final BigDecimal amount = (BigDecimal) row[1];
                if (accountId != null) {
                    balances.put(accountId, balances.getOrDefault(accountId, BigDecimal.ZERO).subtract(amount));
                }
            }

            return balances;
        });
    }

    /** Associates a user with an account. */
    @Transactional
    public UserAccountMetaEntity associateUserWithAccount(final Long userId, final Long accountId) {
        if (UserAccountMetaEntity.existsByUserAndAccount(userId, accountId)) {
            throw new WebApplicationException("User is already associated with this account", Response.Status.CONFLICT);
        }

        final UserEntity user = UserEntity.findById(userId);
        final AccountEntity account = AccountEntity.findById(accountId);

        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        if (account == null) {
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }

        final var meta = new UserAccountMetaEntity();
        meta.user = user;
        meta.account = account;
        meta.persistAndFlush();
        return meta;
    }

    /** Removes a user from an account. */
    @Transactional
    public boolean removeUserFromAccount(final Long userId, final Long accountId) {
        return UserAccountMetaEntity.delete("user.id = ?1 AND account.id = ?2", userId, accountId) > 0;
    }

    /** Returns true if the user is associated with the given account. */
    public boolean isUserAssociatedWithAccount(final Long userId, final Long accountId) {
        return UserAccountMetaEntity.existsByUserAndAccount(userId, accountId);
    }

    /** Returns all accounts for the current user identified by username. */
    public List<AccountResponseDto> getAccountsForCurrentUser(final String username, final int page, final int size) {
        final UserEntity user = UserEntity.find("username", username).firstResult();
        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        final List<AccountEntity> entities = AccountEntity
                .find("SELECT a FROM AccountEntity a JOIN a.userAccountMetas uam WHERE uam.user.id = ?1 ORDER BY a.id",
                        user.id)
                .page(page, size).list();
        return this.accountResponseAssembler.toAccountResponseDtos(entities);
    }

    /**
     * Returns all accounts for the current user identified by username (no pagination support backward compat).
     */
    // NOTE: Results are capped at MAX_ACCOUNTS_LIMIT. Pagination not yet
    // implemented.
    public List<AccountResponseDto> getAccountsForCurrentUser(final String username) {
        return this.getAccountsForCurrentUser(username, 0, MAX_ACCOUNTS_LIMIT);
    }

    /** Returns true if the user has access to the given account. */
    public boolean hasUserAccessToAccount(final String username, final Long accountId) {
        final UserEntity user = UserEntity.find("username", username).firstResult();
        if (user == null) {
            return false;
        }
        return user.id != null && this.isUserAssociatedWithAccount(user.id, accountId);
    }

    /** Returns the balance of the account for the current user. */
    public BigDecimal getMyAccountBalance(final String username, final Long accountId) {
        if (!this.hasUserAccessToAccount(username, accountId)) {
            throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
        }
        return this.getAccountBalance(accountId);
    }

    /**
     * Updates the isNegative flag for the user-account association.
     *
     * @param userId
     *            the user whose association to update
     * @param accountId
     *            the account in the association
     * @param isNegative
     *            the new flag value
     */
    @Transactional
    public void updateUserAccountMeta(final Long userId, final Long accountId, final boolean isNegative) {
        final UserAccountMetaEntity meta = UserAccountMetaEntity.findByUserAndAccount(userId, accountId);
        if (meta == null) {
            throw new WebApplicationException("User-account association not found", Response.Status.NOT_FOUND);
        }
        meta.isNegative = isNegative;
        meta.persistAndFlush();
    }

    /**
     * Returns a map of accountId to isNegative flag for the given user and account list.
     *
     * @param userId
     *            the user whose flags to look up
     * @param accountIds
     *            the accounts to query
     * @return map of accountId to isNegative
     */
    public Map<Long, Boolean> getIsNegativeFlagsForUser(final Long userId, final List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return Map.of();
        }
        final List<UserAccountMetaEntity> metas =
                UserAccountMetaEntity.find("user.id = ?1 AND account.id IN ?2", userId, accountIds).list();
        final Map<Long, Boolean> result = new HashMap<>();
        for (final UserAccountMetaEntity m : metas) {
            final AccountEntity a = m.account;
            if (a != null && a.id != null) {
                result.put(a.id, m.isNegative);
            }
        }
        return result;
    }
}
