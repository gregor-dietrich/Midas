package de.vptr.midas.api.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;

/** Tests the privilege ceiling against the demo ranks: Administrator holds every permission, Guest none. */
@QuarkusTest
@SuppressWarnings("NullAway")
class PrivilegeCeilingTest {

    private static UserRankEntity rank(final String name) {
        return UserRankEntity.find("name", name).firstResult();
    }

    private static void assertRefused(final String message, final Executable check) {
        final var refusal = assertThrows(WebApplicationException.class, check);
        assertEquals(403, refusal.getResponse().getStatus());
        assertEquals(message, refusal.getMessage());
    }

    @Test
    @TestTransaction
    void testWithoutCallerNothingIsRefused() {
        final PrivilegeCeiling ceiling = PrivilegeCeiling.of(null);
        assertSame(PrivilegeCeiling.NONE, ceiling);
        assertDoesNotThrow(() -> ceiling.requireCanManageUser(rank("Administrator")));
        assertDoesNotThrow(() -> ceiling.requireCanGrant(rank("Administrator")));
    }

    @Test
    @TestTransaction
    void testRankAboveCallerIsRefused() {
        final PrivilegeCeiling ceiling = PrivilegeCeiling.of("moderator");
        assertRefused(PrivilegeCeiling.MANAGE_USER_MESSAGE, () -> ceiling.requireCanManageUser(rank("Administrator")));
        assertRefused(PrivilegeCeiling.MANAGE_RANK_MESSAGE, () -> ceiling.requireCanManageRank(rank("Administrator")));
        assertRefused(PrivilegeCeiling.GRANT_MESSAGE, () -> ceiling.requireCanGrant(rank("Administrator")));
    }

    @Test
    @TestTransaction
    void testEqualAndLowerRanksAreAllowed() {
        final PrivilegeCeiling ceiling = PrivilegeCeiling.of("moderator");
        assertDoesNotThrow(() -> ceiling.requireCanManageUser(rank("Moderator")));
        assertDoesNotThrow(() -> ceiling.requireCanGrant(rank("User")));
        assertDoesNotThrow(() -> ceiling.requireCanManageRank(rank("Guest")));
        assertDoesNotThrow(() -> ceiling.requireCanManageUser(null));
    }

    @Test
    @TestTransaction
    void testAdministratorManagesEveryRank() {
        final PrivilegeCeiling ceiling = PrivilegeCeiling.of("admin");
        assertDoesNotThrow(() -> ceiling.requireCanManageUser(rank("Administrator")));
        assertDoesNotThrow(() -> ceiling.requireCanGrant(rank("Moderator")));
    }

    @Test
    @TestTransaction
    void testCallerWithoutPermissionsManagesOnlyEmptyRanks() {
        final PrivilegeCeiling ceiling = PrivilegeCeiling.of("guest");
        assertDoesNotThrow(() -> ceiling.requireCanManageUser(rank("Guest")));
        assertRefused(PrivilegeCeiling.MANAGE_USER_MESSAGE, () -> ceiling.requireCanManageUser(rank("User")));
    }

    @Test
    @TestTransaction
    void testCeilingIsReadBeforeTheChange() {
        final PrivilegeCeiling ceiling = PrivilegeCeiling.of("moderator");
        final UserRankEntity own = rank("Moderator");
        own.pageAdd = true;
        assertRefused(PrivilegeCeiling.GRANT_MESSAGE, () -> ceiling.requireCanGrant(own));
    }

    @Test
    @TestTransaction
    void testBannedCallerIsRefused() {
        UserEntity.<UserEntity>find("username", "moderator").firstResult().banned = true;
        assertRefused(PrivilegeCeiling.CALLER_INACTIVE_MESSAGE, () -> PrivilegeCeiling.of("moderator"));
    }

    @Test
    @TestTransaction
    void testDeactivatedCallerIsRefused() {
        UserEntity.<UserEntity>find("username", "moderator").firstResult().activated = false;
        assertRefused(PrivilegeCeiling.CALLER_INACTIVE_MESSAGE, () -> PrivilegeCeiling.of("moderator"));
    }

    @Test
    @TestTransaction
    void testUnknownCallerIsRefused() {
        assertRefused(PrivilegeCeiling.CALLER_INACTIVE_MESSAGE, () -> PrivilegeCeiling.of("no-such-user"));
    }
}
