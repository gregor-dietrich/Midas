package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.createUniqueTestName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.AccountDto;
import de.vptr.midas.api.entity.UserAccountMetaEntity;
import de.vptr.midas.api.entity.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class AccountAccessServiceTest {
    @Inject
    AccountAccessService accountAccessService;

    @Inject
    AccountService accountService;

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.accountAccessService);
    }

    @Test
    void testGetAssociatedUsersForNonExistentAccount() {
        try {
            this.accountAccessService.getAssociatedUsers(999999L);
        } catch (final WebApplicationException e) {
            assertNotNull(e);
        }
    }

    @Test
    void testIsUserAssociatedWithAccountNonExistent() {
        final boolean result = this.accountAccessService.isUserAssociatedWithAccount(999999L, 999999L);
        assert !result;
    }

    @Test
    void testHasUserAccessToAccountNonExistentUser() {
        final boolean result = this.accountAccessService.hasUserAccessToAccount("nonexistent_user_xyz", 999999L);
        assert !result;
    }

    @Test
    @TestTransaction
    void updateUserAccountMeta_setsIsNegativeFlagOnExistingAssociation() {
        final UserEntity user = UserEntity.findById(1L);
        assertNotNull(user);

        final AccountDto dto = new AccountDto();
        dto.name = createUniqueTestName("Meta Account");
        final var created = this.accountService.createAccount(dto);

        final Long userId = Objects.requireNonNull(user.id, "user.id");
        final Long accountId = Objects.requireNonNull(created.id, "created.id");
        this.accountAccessService.associateUserWithAccount(userId, accountId);
        this.accountAccessService.updateUserAccountMeta(userId, accountId, true);

        final UserAccountMetaEntity meta = UserAccountMetaEntity.findByUserAndAccount(userId, accountId);
        assertNotNull(meta);
        assertTrue(meta.isNegative);
    }

    @Test
    void updateUserAccountMeta_throwsNotFoundForMissingAssociation() {
        final WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.accountAccessService.updateUserAccountMeta(999999L, 999999L, true));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void getIsNegativeFlagsForUser_returnsCorrectFlagsForAssociatedAccounts() {
        final UserEntity user = UserEntity.findById(1L);
        assertNotNull(user);

        final AccountDto dto = new AccountDto();
        dto.name = createUniqueTestName("Flags Account");
        final var created = this.accountService.createAccount(dto);

        final Long userId2 = Objects.requireNonNull(user.id, "user.id");
        final Long accountId2 = Objects.requireNonNull(created.id, "created.id");
        this.accountAccessService.associateUserWithAccount(userId2, accountId2);
        this.accountAccessService.updateUserAccountMeta(userId2, accountId2, true);

        final Map<Long, Boolean> flags =
                this.accountAccessService.getIsNegativeFlagsForUser(userId2, List.of(accountId2));

        assertNotNull(flags);
        assertEquals(1, flags.size());
        assertTrue(Boolean.TRUE.equals(flags.get(accountId2)));
    }

    @Test
    void getIsNegativeFlagsForUser_returnsEmptyMapForEmptyAccountList() {
        final Map<Long, Boolean> flags = this.accountAccessService.getIsNegativeFlagsForUser(1L, List.of());
        assertNotNull(flags);
        assertTrue(flags.isEmpty());
    }
}
