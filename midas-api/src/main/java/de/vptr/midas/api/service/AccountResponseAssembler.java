package de.vptr.midas.api.service;

import java.util.List;
import java.util.Map;

import de.vptr.midas.api.dto.AccountResponseDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.UserAccountMetaEntity;
import de.vptr.midas.api.util.QueryAggregationUtil;
import jakarta.enterprise.context.ApplicationScoped;

/** Builds account response DTOs with batched association lookups. */
@ApplicationScoped
public class AccountResponseAssembler {

    /** Converts one account entity into a response DTO. */
    public AccountResponseDto toAccountResponseDto(final AccountEntity account) {
        return this.toAccountResponseDtos(List.of(account)).getFirst();
    }

    /** Converts account entities into response DTOs with shared lookup queries. */
    public List<AccountResponseDto> toAccountResponseDtos(final List<AccountEntity> accounts) {
        if (accounts.isEmpty()) {
            return List.of();
        }

        final List<Long> accountIds = accounts.stream().map(account -> account.id).toList();
        final Map<Long, List<Long>> userIdsByAccount = QueryAggregationUtil.groupLongValuesByKey(UserAccountMetaEntity
                .getEntityManager()
                .createQuery("SELECT m.account.id, m.user.id FROM UserAccountMetaEntity m WHERE m.account.id IN :ids",
                        Object[].class)
                .setParameter("ids", accountIds).getResultList());

        return accounts.stream().map(account -> {
            final List<Long> userIds = userIdsByAccount.getOrDefault(account.id, List.of());
            return new AccountResponseDto(account, userIds.size(), userIds);
        }).toList();
    }
}
