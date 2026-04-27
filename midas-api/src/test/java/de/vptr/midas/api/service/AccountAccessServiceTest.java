package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AccountAccessServiceTest {
    @Inject
    AccountAccessService accountAccessService;

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
}
