package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.TestUtil.ADMIN_USERNAME;
import static de.vptr.midas.api.util.TestUtil.GUEST_USERNAME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AdminAccessServiceTest {

    @Inject
    AdminAccessService adminAccessService;

    @Test
    void testHasAdminViewAccessForAdminUser() {
        assertTrue(this.adminAccessService.hasAdminViewAccess(ADMIN_USERNAME));
    }

    @Test
    void testHasAdminViewAccessForGuestUser() {
        assertFalse(this.adminAccessService.hasAdminViewAccess(GUEST_USERNAME));
    }

    @Test
    void testHasAdminViewAccessForUnknownUser() {
        assertFalse(this.adminAccessService.hasAdminViewAccess("unknown-user"));
    }

    @Test
    void testHasAdminViewAccessForNullUsername() {
        assertFalse(this.adminAccessService.hasAdminViewAccess(null));
    }
}
