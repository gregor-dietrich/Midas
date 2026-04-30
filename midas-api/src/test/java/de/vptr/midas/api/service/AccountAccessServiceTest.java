package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.createUniqueTestName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

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
        } catch (final jakarta.ws.rs.WebApplicationException e) {
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

        this.accountAccessService.associateUserWithAccount(user.id, created.id);
        this.accountAccessService.updateUserAccountMeta(user.id, created.id, true);

        final UserAccountMetaEntity meta = UserAccountMetaEntity
                .findByUserAndAccount(user.id, created.id);
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

        this.accountAccessService.associateUserWithAccount(user.id, created.id);
        this.accountAccessService.updateUserAccountMeta(user.id, created.id, true);

        final Map<Long, Boolean> flags = this.accountAccessService
                .getIsNegativeFlagsForUser(user.id, List.of(created.id));

        assertNotNull(flags);
        assertEquals(1, flags.size());
        assertTrue(flags.get(created.id));
    }

    @Test
    void getIsNegativeFlagsForUser_returnsEmptyMapForEmptyAccountList() {
        final Map<Long, Boolean> flags = this.accountAccessService
                .getIsNegativeFlagsForUser(1L, List.of());
        assertNotNull(flags);
        assertTrue(flags.isEmpty());
    }
}
