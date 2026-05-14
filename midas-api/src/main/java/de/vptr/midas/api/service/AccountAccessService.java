package de.vptr.midas.api.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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
    public List<UserResponseDto> getAssociatedUsers(final String accountPublicId) {
        final AccountEntity account = AccountEntity.find("publicId", accountPublicId).firstResult();
        if (account == null) {
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }
        final List<UserEntity> users = UserEntity.find("""
                SELECT u FROM UserEntity u LEFT JOIN FETCH u.rank WHERE u.id IN \
                (SELECT m.user.id FROM UserAccountMetaEntity m WHERE m.account.id = ?1)""", account.id).list();
        if (users.isEmpty()) {
            return List.of();
        }
        final List<Long> userIds = users.stream().map(u -> Objects.requireNonNull(u.id)).toList();

        final List<Object[]> postResults = PostEntity.getEntityManager()
                .createQuery("SELECT p.user.id, COUNT(p) FROM PostEntity p WHERE p.user.id IN :ids GROUP BY p.user.id",
                        Object[].class)
                .setParameter("ids", userIds).getResultList();
        final Map<Long, Long> postCounts = QueryAggregationUtil.toLongCountMap(postResults);

        final List<Object[]> commentResults = PostCommentEntity.getEntityManager().createQuery(
                "SELECT c.user.id, COUNT(c) FROM PostCommentEntity c WHERE c.user.id IN :ids GROUP BY c.user.id",
                Object[].class).setParameter("ids", userIds).getResultList();
        final Map<Long, Long> commentCounts = QueryAggregationUtil.toLongCountMap(commentResults);

        final List<UserAccountMetaEntity> metas =
                UserAccountMetaEntity.findByAccountId(Objects.requireNonNull(account.id));
        final Map<Long, Boolean> isNegativeByUser = new HashMap<>();
        final Map<Long, Boolean> canManageByUser = new HashMap<>();
        for (final UserAccountMetaEntity m : metas) {
            final UserEntity u = m.user;
            if (u != null && u.id != null) {
                isNegativeByUser.put(u.id, m.isNegative);
                canManageByUser.put(u.id, m.canManage);
            }
        }

        return users.stream().map(u -> {
            final UserResponseDto dto =
                    new UserResponseDto(u, postCounts.getOrDefault(u.id, 0L), commentCounts.getOrDefault(u.id, 0L));
            dto.isNegative = isNegativeByUser.getOrDefault(u.id, false);
            dto.canManage = canManageByUser.getOrDefault(u.id, false);
            return dto;
        }).toList();
    }

    /** Returns all accounts for the given user. */
    public List<AccountResponseDto> getAccountsForUser(final String userPublicId) {
        return PerformanceMonitor.time("getAccountsForUser(" + userPublicId + ")", () -> {
            final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
            if (user == null) {
                throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
            }

            final List<AccountEntity> accounts = AccountEntity.find("""
                    SELECT a FROM AccountEntity a WHERE a.id IN \
                    (SELECT m.account.id FROM UserAccountMetaEntity m WHERE m.user.id = ?1)""", user.id).list();
            return this.accountResponseAssembler.toAccountResponseDtos(accounts);
        });
    }

    /** Returns all payments for the given account. */
    public List<PaymentResponseDto> getAccountPayments(final String accountPublicId) {
        return PerformanceMonitor.time("getAccountPayments(" + accountPublicId + ")", () -> {
            @SuppressWarnings("unchecked")
            final List<PaymentEntity> payments = PaymentEntity.getEntityManager().createQuery("""
                    SELECT p FROM PaymentEntity p \
                    LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user \
                    WHERE p.sourceAccount.publicId = :publicId OR p.targetAccount.publicId = :publicId \
                    ORDER BY p.date DESC""").setParameter("publicId", accountPublicId).getResultList();

            return payments.stream().map(PaymentResponseDto::new).toList();
        });
    }

    private static final int MAX_PAYMENTS = 1_000;

    /** Returns all outgoing payments for the given account. */
    public List<PaymentResponseDto> getOutgoingPayments(final String accountPublicId) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.sourceAccount.publicId = ?1",
                accountPublicId).page(0, MAX_PAYMENTS).list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns all incoming payments for the given account. */
    public List<PaymentResponseDto> getIncomingPayments(final String accountPublicId) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.targetAccount.publicId = ?1",
                accountPublicId).page(0, MAX_PAYMENTS).list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns the current balance for the given account. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public BigDecimal getAccountBalance(final String accountPublicId) {
        return PerformanceMonitor.time("getAccountBalance(" + accountPublicId + ")", () -> {
            final BigDecimal incoming = PaymentEntity.getEntityManager().createQuery("""
                    SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p \
                    JOIN p.targetAccount t \
                    WHERE t.publicId = :publicId""", BigDecimal.class)
                    .setParameter("publicId", accountPublicId).getSingleResult();
            final BigDecimal outgoing = PaymentEntity.getEntityManager().createQuery("""
                    SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p \
                    JOIN p.sourceAccount s \
                    WHERE s.publicId = :publicId""", BigDecimal.class)
                    .setParameter("publicId", accountPublicId).getSingleResult();
            return incoming.subtract(outgoing);
        });
    }

    /** Returns balances for the given list of account publicIds. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<String, BigDecimal> getAccountBalances(final List<String> accountPublicIds) {
        return PerformanceMonitor.time("getAccountBalances(" + accountPublicIds.size() + " accounts)", () -> {
            if (accountPublicIds == null || accountPublicIds.isEmpty()) {
                return Map.of();
            }

            final Map<String, BigDecimal> balances = new HashMap<>();
            accountPublicIds.forEach(id -> balances.put(id, BigDecimal.ZERO));

            // Compute incoming payments (target) aggregated by account
            final List<Object[]> incomingResults = PaymentEntity.getEntityManager().createQuery("""
                    SELECT p.targetAccount.publicId, COALESCE(SUM(p.amount), 0) FROM PaymentEntity p \
                    WHERE p.targetAccount.publicId IN :publicIds GROUP BY p.targetAccount.publicId""", Object[].class)
                    .setParameter("publicIds", accountPublicIds).getResultList();

            for (final Object[] row : incomingResults) {
                final String publicId = (String) row[0];
                final BigDecimal amount = (BigDecimal) row[1];
                if (publicId != null) {
                    balances.put(publicId, balances.getOrDefault(publicId, BigDecimal.ZERO).add(amount));
                }
            }

            // Compute outgoing payments (source) aggregated by account
            final List<Object[]> outgoingResults = PaymentEntity.getEntityManager().createQuery("""
                    SELECT p.sourceAccount.publicId, COALESCE(SUM(p.amount), 0) FROM PaymentEntity p \
                    WHERE p.sourceAccount.publicId IN :publicIds GROUP BY p.sourceAccount.publicId""", Object[].class)
                    .setParameter("publicIds", accountPublicIds).getResultList();

            for (final Object[] row : outgoingResults) {
                final String publicId = (String) row[0];
                final BigDecimal amount = (BigDecimal) row[1];
                if (publicId != null) {
                    balances.put(publicId, balances.getOrDefault(publicId, BigDecimal.ZERO).subtract(amount));
                }
            }

            return balances;
        });
    }

    /** Associates a user with an account. */
    @Transactional
    public UserAccountMetaEntity associateUserWithAccount(final String userPublicId, final String accountPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        final AccountEntity account = AccountEntity.find("publicId", accountPublicId).firstResult();

        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        if (account == null) {
            throw new WebApplicationException("Account not found", Response.Status.NOT_FOUND);
        }

        if (UserAccountMetaEntity.existsByUserAndAccount(Objects.requireNonNull(user.id),
                Objects.requireNonNull(account.id))) {
            throw new WebApplicationException("User is already associated with this account", Response.Status.CONFLICT);
        }

        final var meta = new UserAccountMetaEntity();
        meta.user = user;
        meta.account = account;
        meta.persistAndFlush();
        return meta;
    }

    /** Removes a user from an account. */
    @Transactional
    public boolean removeUserFromAccount(final String userPublicId, final String accountPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        final AccountEntity account = AccountEntity.find("publicId", accountPublicId).firstResult();
        if (user == null || account == null) {
            return false;
        }
        return UserAccountMetaEntity.delete("user.id = ?1 AND account.id = ?2", user.id, account.id) > 0;
    }

    /** Returns true if the user is associated with the given account. */
    public boolean isUserAssociatedWithAccount(final String userPublicId, final String accountPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        final AccountEntity account = AccountEntity.find("publicId", accountPublicId).firstResult();
        if (user == null || account == null || user.id == null || account.id == null) {
            return false;
        }
        return UserAccountMetaEntity.existsByUserAndAccount(user.id, account.id);
    }

    /** Returns all accounts for the current user identified by username. */
    public List<AccountResponseDto> getAccountsForCurrentUser(final String username, final int page, final int size) {
        final UserEntity user = UserEntity.find("username", username).firstResult();
        if (user == null) {
            throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
        }
        final List<AccountEntity> entities = AccountEntity.find(
                "SELECT a FROM AccountEntity a JOIN a.userAccountMetas uam WHERE uam.user.id = ?1 ORDER BY a.publicId",
                user.id).page(page, size).list();
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
    public boolean hasUserAccessToAccount(final String username, final String accountPublicId) {
        final UserEntity user = UserEntity.find("username", username).firstResult();
        if (user == null) {
            return false;
        }
        return user.id != null
                && this.isUserAssociatedWithAccount(Objects.requireNonNull(user.publicId), accountPublicId);
    }

    /** Returns the balance of the account for the current user. */
    public BigDecimal getMyAccountBalance(final String username, final String accountPublicId) {
        if (!this.hasUserAccessToAccount(username, accountPublicId)) {
            throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
        }
        return this.getAccountBalance(accountPublicId);
    }

    /**
     * Updates the meta flags (isNegative, canManage) for the user-account association.
     *
     * @param userPublicId
     *            the user whose association to update
     * @param accountPublicId
     *            the account in the association
     * @param isNegative
     *            the new isNegative flag value
     * @param canManage
     *            the new canManage flag value
     */
    @Transactional
    public void updateUserAccountMeta(final String userPublicId, final String accountPublicId, final boolean isNegative,
            final boolean canManage) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        final AccountEntity account = AccountEntity.find("publicId", accountPublicId).firstResult();
        if (user == null || account == null) {
            throw new WebApplicationException("User or Account not found", Response.Status.NOT_FOUND);
        }

        final UserAccountMetaEntity meta = UserAccountMetaEntity.findByUserAndAccount(Objects.requireNonNull(user.id),
                Objects.requireNonNull(account.id));
        if (meta == null) {
            throw new WebApplicationException("User-account association not found", Response.Status.NOT_FOUND);
        }
        meta.isNegative = isNegative;
        meta.canManage = canManage;
        meta.persistAndFlush();
    }

    /**
     * Returns a map of accountPublicId to isNegative flag for the given user and account list.
     *
     * @param userInternalId
     *            the internal ID of the user whose flags to look up
     * @param accountPublicIds
     *            the account public IDs to query
     * @return map of accountPublicId to isNegative
     */
    public Map<String, Boolean> getIsNegativeFlagsForUser(final Long userInternalId,
            final List<String> accountPublicIds) {
        return this.getMetaFlagsForUser(userInternalId, accountPublicIds, m -> m.isNegative);
    }

    /**
     * Returns a map of accountPublicId to canManage flag for the given user and account list.
     *
     * @param userInternalId
     *            the internal ID of the user whose flags to look up
     * @param accountPublicIds
     *            the account public IDs to query
     * @return map of accountPublicId to canManage
     */
    public Map<String, Boolean> getCanManageFlagsForUser(final Long userInternalId,
            final List<String> accountPublicIds) {
        return this.getMetaFlagsForUser(userInternalId, accountPublicIds, m -> m.canManage);
    }

    private Map<String, Boolean> getMetaFlagsForUser(final Long userInternalId, final List<String> accountPublicIds,
            final Function<UserAccountMetaEntity, Boolean> flagExtractor) {
        if (accountPublicIds == null || accountPublicIds.isEmpty()) {
            return Map.of();
        }
        final List<UserAccountMetaEntity> metas = UserAccountMetaEntity
                .find("user.id = ?1 AND account.publicId IN ?2", userInternalId, accountPublicIds).list();
        final Map<String, Boolean> result = new HashMap<>();
        for (final UserAccountMetaEntity m : metas) {
            final AccountEntity a = m.account;
            if (a != null && a.publicId != null) {
                result.put(a.publicId, flagExtractor.apply(m));
            }
        }
        return result;
    }

    /** Returns true if the user can manage (add, edit, delete) payments for this account. */
    public boolean canUserManage(final String username, final String accountPublicId) {
        final UserEntity user = UserEntity.find("username", username).firstResult();
        final AccountEntity account = AccountEntity.find("publicId", accountPublicId).firstResult();
        if (user == null || account == null || user.id == null || account.id == null) {
            return false;
        }
        final UserAccountMetaEntity meta = UserAccountMetaEntity.findByUserAndAccount(user.id, account.id);
        return meta != null && meta.canManage;
    }
}
