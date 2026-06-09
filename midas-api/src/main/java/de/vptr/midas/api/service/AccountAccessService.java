package de.vptr.midas.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import de.vptr.midas.api.dto.AccountResponseDto;
import de.vptr.midas.api.dto.BalancePointDto;
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
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.annotation.Nullable;
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

    /**
     * Returns a single page of payments for the given account, optionally restricted to a date range (inclusive) and a
     * free-text query (matched against comment, source and target account names). Filtering and pagination are applied
     * server-side so callers never load the full payment set into memory.
     */
    public List<PaymentResponseDto> getAccountPayments(final String accountPublicId,
            @Nullable final LocalDate startDate, @Nullable final LocalDate endDate, @Nullable final String query,
            final int page, final int size) {
        return PerformanceMonitor.time("getAccountPayments(paged," + accountPublicId + ")", () -> {
            final boolean hasRange = startDate != null && endDate != null;
            final String like = SearchQueryUtil.toContainsLikePattern(query);

            final StringBuilder jpql = new StringBuilder("""
                    SELECT p FROM PaymentEntity p \
                    LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user \
                    WHERE (p.sourceAccount.publicId = :publicId OR p.targetAccount.publicId = :publicId)""");
            if (hasRange) {
                jpql.append(" AND p.date BETWEEN :startDate AND :endDate");
            }
            if (like != null) {
                jpql.append(" AND (LOWER(p.comment) LIKE :like ESCAPE '\\'")
                        .append(" OR LOWER(p.sourceAccount.name) LIKE :like ESCAPE '\\'")
                        .append(" OR LOWER(p.targetAccount.name) LIKE :like ESCAPE '\\')");
            }
            jpql.append(" ORDER BY p.date DESC");

            final var jpaQuery = PaymentEntity.getEntityManager().createQuery(jpql.toString(), PaymentEntity.class)
                    .setParameter("publicId", accountPublicId);
            if (hasRange) {
                jpaQuery.setParameter("startDate", startDate).setParameter("endDate", endDate);
            }
            if (like != null) {
                jpaQuery.setParameter("like", like);
            }
            jpaQuery.setFirstResult(page * size).setMaxResults(size);

            return jpaQuery.getResultList().stream().map(PaymentResponseDto::new).toList();
        });
    }

    /** Returns all payments grouped by account publicId for the given accounts. */
    public Map<String, List<PaymentResponseDto>> getPaymentsByAccount(final List<String> accountPublicIds) {
        return PerformanceMonitor.time("getPaymentsByAccount(" + accountPublicIds.size() + " accounts)", () -> {
            final Map<String, List<PaymentResponseDto>> grouped = new HashMap<>();
            accountPublicIds.forEach(id -> grouped.put(id, new ArrayList<>()));
            if (accountPublicIds.isEmpty()) {
                return grouped;
            }

            @SuppressWarnings("unchecked")
            final List<PaymentEntity> payments = PaymentEntity.getEntityManager().createQuery("""
                    SELECT p FROM PaymentEntity p \
                    LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user \
                    WHERE p.sourceAccount.publicId IN :publicIds OR p.targetAccount.publicId IN :publicIds \
                    ORDER BY p.date DESC""").setParameter("publicIds", accountPublicIds).getResultList();

            for (final PaymentEntity p : payments) {
                final PaymentResponseDto dto = new PaymentResponseDto(p);
                final AccountEntity source = p.sourceAccount;
                final AccountEntity target = p.targetAccount;
                if (source != null && source.publicId != null && grouped.containsKey(source.publicId)) {
                    grouped.get(source.publicId).add(dto);
                }
                if (target != null && target.publicId != null && grouped.containsKey(target.publicId)
                        && (source == null || !Objects.equals(source.publicId, target.publicId))) {
                    grouped.get(target.publicId).add(dto);
                }
            }
            return grouped;
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

    /** Returns the current all-time balance for the given account. */
    public BigDecimal getAccountBalance(final String accountPublicId) {
        return this.getAccountBalance(accountPublicId, null, null);
    }

    /**
     * Returns the balance for the given account. When both {@code startDate} and {@code endDate} are provided, only
     * payments whose date falls within that inclusive range are summed; otherwise the all-time balance is returned.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public BigDecimal getAccountBalance(final String accountPublicId, @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        return PerformanceMonitor.time("getAccountBalance(" + accountPublicId + ")", () -> {
            final BigDecimal incoming = this.sumByAccount("targetAccount", accountPublicId, startDate, endDate);
            final BigDecimal outgoing = this.sumByAccount("sourceAccount", accountPublicId, startDate, endDate);
            return incoming.subtract(outgoing);
        });
    }

    /** Sums payment amounts for a single account on the given association side, optionally restricted by date range. */
    private BigDecimal sumByAccount(final String joinPath, final String accountPublicId,
            @Nullable final LocalDate startDate, @Nullable final LocalDate endDate) {
        final boolean hasRange = startDate != null && endDate != null;
        final String dateClause = hasRange ? " AND p.date BETWEEN :startDate AND :endDate" : "";
        final var query = PaymentEntity.getEntityManager()
                .createQuery("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p JOIN p." + joinPath
                        + " a WHERE a.publicId = :publicId" + dateClause, BigDecimal.class)
                .setParameter("publicId", accountPublicId);
        if (hasRange) {
            query.setParameter("startDate", startDate).setParameter("endDate", endDate);
        }
        return query.getSingleResult();
    }

    /** Returns all-time balances for the given list of account publicIds. */
    public Map<String, BigDecimal> getAccountBalances(final List<String> accountPublicIds) {
        return this.getAccountBalances(accountPublicIds, null, null);
    }

    /**
     * Returns balances for the given account publicIds. When both {@code startDate} and {@code endDate} are provided,
     * only payments within that inclusive range are summed; otherwise all-time balances are returned.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<String, BigDecimal> getAccountBalances(final List<String> accountPublicIds,
            @Nullable final LocalDate startDate, @Nullable final LocalDate endDate) {
        return PerformanceMonitor.time("getAccountBalances(" + accountPublicIds.size() + " accounts)", () -> {
            if (accountPublicIds == null || accountPublicIds.isEmpty()) {
                return Map.of();
            }

            final Map<String, BigDecimal> balances = new HashMap<>();
            accountPublicIds.forEach(id -> balances.put(id, BigDecimal.ZERO));

            this.aggregateBalances(balances, "targetAccount", true, accountPublicIds, startDate, endDate);
            this.aggregateBalances(balances, "sourceAccount", false, accountPublicIds, startDate, endDate);

            return balances;
        });
    }

    /**
     * Aggregates summed payment amounts grouped by account for one association side and folds them into
     * {@code balances} ({@code add == true} for incoming/target, {@code false} to subtract outgoing/source).
     */
    private void aggregateBalances(final Map<String, BigDecimal> balances, final String joinPath, final boolean add,
            final List<String> accountPublicIds, @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        final boolean hasRange = startDate != null && endDate != null;
        final String dateClause = hasRange ? " AND p.date BETWEEN :startDate AND :endDate" : "";
        final var query = PaymentEntity.getEntityManager().createQuery(
                "SELECT p." + joinPath + ".publicId, COALESCE(SUM(p.amount), 0) FROM PaymentEntity p " + "WHERE p."
                        + joinPath + ".publicId IN :publicIds" + dateClause + " GROUP BY p." + joinPath + ".publicId",
                Object[].class).setParameter("publicIds", accountPublicIds);
        if (hasRange) {
            query.setParameter("startDate", startDate).setParameter("endDate", endDate);
        }
        for (final Object[] row : query.getResultList()) {
            final String publicId = (String) row[0];
            final BigDecimal amount = (BigDecimal) row[1];
            if (publicId != null) {
                final BigDecimal current = balances.getOrDefault(publicId, BigDecimal.ZERO);
                balances.put(publicId, add ? current.add(amount) : current.subtract(amount));
            }
        }
    }

    /**
     * Returns the cumulative balance over time for a single account, with one point per distinct payment date. The
     * running balance starts at zero at the beginning of the (optional) date range and only accumulates payments inside
     * it, mirroring {@link #getAccountBalance(String, LocalDate, LocalDate)}. The per-user {@code isNegative} flag is
     * not applied (matching the raw single-account balance figure).
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<BalancePointDto> getAccountBalanceHistory(final String accountPublicId,
            @Nullable final LocalDate startDate, @Nullable final LocalDate endDate) {
        return PerformanceMonitor.time("getAccountBalanceHistory(" + accountPublicId + ")", () -> {
            final List<String> accountPublicIds = List.of(accountPublicId);
            final NavigableMap<LocalDate, BigDecimal> dailyDeltas = new TreeMap<>();
            this.aggregateSignedDailyDeltas(dailyDeltas, "targetAccount", true, accountPublicIds, Map.of(), startDate,
                    endDate);
            this.aggregateSignedDailyDeltas(dailyDeltas, "sourceAccount", false, accountPublicIds, Map.of(), startDate,
                    endDate);
            return toCumulativeSeries(dailyDeltas);
        });
    }

    /**
     * Returns the cumulative total balance over time across all accounts of the current user, with one point per
     * distinct payment date. Each account's contribution is signed by its per-user {@code isNegative} flag, so the
     * series equals the sum of the effective balances shown on the account overview.
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<BalancePointDto> getTotalBalanceHistory(final String username, @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        return PerformanceMonitor.time("getTotalBalanceHistory(" + username + ")", () -> {
            final UserEntity user = UserEntity.find("username", username).firstResult();
            if (user == null) {
                throw new WebApplicationException("User not found", Response.Status.NOT_FOUND);
            }
            final List<AccountResponseDto> accounts = this.getAccountsForCurrentUser(username);
            if (accounts.isEmpty()) {
                return List.of();
            }
            final List<String> accountPublicIds = accounts.stream().map(account -> account.publicId).toList();
            final Map<String, Boolean> isNegativeFlags =
                    this.getIsNegativeFlagsForUser(Objects.requireNonNull(user.id), accountPublicIds);

            final NavigableMap<LocalDate, BigDecimal> dailyDeltas = new TreeMap<>();
            this.aggregateSignedDailyDeltas(dailyDeltas, "targetAccount", true, accountPublicIds, isNegativeFlags,
                    startDate, endDate);
            this.aggregateSignedDailyDeltas(dailyDeltas, "sourceAccount", false, accountPublicIds, isNegativeFlags,
                    startDate, endDate);
            return toCumulativeSeries(dailyDeltas);
        });
    }

    /**
     * Aggregates per-date payment sums grouped by account on one association side and folds them into
     * {@code dailyDeltas}. Each account's amount is signed by its {@code isNegative} flag and by the side
     * ({@code incoming == true} adds target amounts, {@code false} subtracts source amounts).
     */
    private void aggregateSignedDailyDeltas(final NavigableMap<LocalDate, BigDecimal> dailyDeltas,
            final String joinPath, final boolean incoming, final List<String> accountPublicIds,
            final Map<String, Boolean> isNegativeFlags, @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        final boolean hasRange = startDate != null && endDate != null;
        final String dateClause = hasRange ? " AND p.date BETWEEN :startDate AND :endDate" : "";
        final var query = PaymentEntity.getEntityManager()
                .createQuery(
                        "SELECT a.publicId, p.date, COALESCE(SUM(p.amount), 0) FROM PaymentEntity p JOIN p." + joinPath
                                + " a WHERE a.publicId IN :publicIds" + dateClause + " GROUP BY a.publicId, p.date",
                        Object[].class)
                .setParameter("publicIds", accountPublicIds);
        if (hasRange) {
            query.setParameter("startDate", startDate).setParameter("endDate", endDate);
        }
        for (final Object[] row : query.getResultList()) {
            final String publicId = (String) row[0];
            final LocalDate date = (LocalDate) row[1];
            final BigDecimal amount = (BigDecimal) row[2];
            if (publicId == null || date == null) {
                continue;
            }
            BigDecimal contribution = amount;
            if (Boolean.TRUE.equals(isNegativeFlags.get(publicId))) {
                contribution = contribution.negate();
            }
            if (!incoming) {
                contribution = contribution.negate();
            }
            dailyDeltas.merge(date, contribution, BigDecimal::add);
        }
    }

    /** Folds an ordered map of per-date deltas into a cumulative running-balance series. */
    private static List<BalancePointDto> toCumulativeSeries(final NavigableMap<LocalDate, BigDecimal> dailyDeltas) {
        final List<BalancePointDto> series = new ArrayList<>(dailyDeltas.size());
        BigDecimal running = BigDecimal.ZERO;
        for (final Map.Entry<LocalDate, BigDecimal> entry : dailyDeltas.entrySet()) {
            running = running.add(entry.getValue());
            series.add(new BalancePointDto(entry.getKey(), running));
        }
        return series;
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
