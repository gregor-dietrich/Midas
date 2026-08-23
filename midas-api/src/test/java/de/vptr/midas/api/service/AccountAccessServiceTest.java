package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestDataBuilder.createUniqueUserDto;
import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.createUniqueTestName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.AccountDto;
import de.vptr.midas.api.dto.BalancePointDto;
import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.dto.PaymentResponseDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.UserAccountMetaEntity;
import de.vptr.midas.api.entity.UserEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class AccountAccessServiceTest {
    @Inject
    AccountAccessService accountAccessService;

    @Inject
    AccountService accountService;

    @Inject
    UserService userService;

    @Inject
    PaymentService paymentService;

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.accountAccessService);
    }

    @Test
    void testGetAssociatedUsersForNonExistentAccount() {
        try {
            this.accountAccessService.getAssociatedUsers("non-existent-account");
        } catch (final WebApplicationException e) {
            assertNotNull(e);
        }
    }

    @Test
    void testIsUserAssociatedWithAccountNonExistent() {
        final boolean result =
                this.accountAccessService.isUserAssociatedWithAccount("non-existent-user", "non-existent-account");
        assertFalse(result);
    }

    @Test
    void testHasUserAccessToAccountNonExistentUser() {
        final boolean result =
                this.accountAccessService.hasUserAccessToAccount("nonexistent_user_xyz", "non-existent-account");
        assertFalse(result);
    }

    @Test
    @TestTransaction
    void updateUserAccountMeta_setsIsNegativeFlagOnExistingAssociation() {
        final UserEntity user = UserEntity.findById(1L);
        assertNotNull(user);
        final String accountPublicId = this.createAndAssociateAccount(user);

        this.accountAccessService.updateUserAccountMeta(Objects.requireNonNull(user.publicId), accountPublicId, true,
                false);

        final UserAccountMetaEntity meta = UserAccountMetaEntity.findByUserAndAccount(Objects.requireNonNull(user.id),
                this.getAccountIdByPublicId(accountPublicId));
        assertNotNull(meta);
        assertTrue(meta.isNegative);
        assertFalse(meta.canManage);
    }

    @Test
    void updateUserAccountMeta_throwsNotFoundForMissingAssociation() {
        final WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.accountAccessService.updateUserAccountMeta("user-999", "account-999", true, false));
        assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void getIsNegativeFlagsForUser_returnsCorrectFlagsForAssociatedAccounts() {
        final UserEntity user = UserEntity.findById(1L);
        assertNotNull(user);
        final String accountPublicId = this.createAndAssociateAccount(user);

        this.accountAccessService.updateUserAccountMeta(Objects.requireNonNull(user.publicId), accountPublicId, true,
                false);

        final Map<String, Boolean> flags = this.accountAccessService
                .getIsNegativeFlagsForUser(Objects.requireNonNull(user.id), List.of(accountPublicId));

        assertNotNull(flags);
        assertEquals(Map.of(accountPublicId, true), flags);
    }

    @Test
    @TestTransaction
    void getAccountBalance_includesPaymentsWithoutCounterparty() {
        final var user = this.userService.createUser(createUniqueUserDto());
        final String accountPublicId = this.createAndAssociateAccount(user);

        // Incoming payment with no source (counterparty)
        final var p1 = new PaymentDto();
        p1.targetAccountPublicId = accountPublicId;
        p1.amount = new BigDecimal("100.00");
        p1.comment = "Incoming no source";
        p1.date = LocalDate.now(ZoneId.systemDefault());
        this.paymentService.createPayment(p1, Objects.requireNonNull(user.username));

        // Outgoing payment with no target (counterparty)
        final var p2 = new PaymentDto();
        p2.sourceAccountPublicId = accountPublicId;
        p2.amount = new BigDecimal("40.00");
        p2.comment = "Outgoing no target";
        p2.date = LocalDate.now(ZoneId.systemDefault());
        this.paymentService.createPayment(p2, Objects.requireNonNull(user.username));

        final BigDecimal balance = this.accountAccessService.getAccountBalance(accountPublicId);
        assertEquals(new BigDecimal("60.00"), balance);
    }

    @Test
    @TestTransaction
    void getAccountBalance_restrictsToDateRange() {
        final var user = this.userService.createUser(createUniqueUserDto());
        final String accountPublicId = this.createAccountWithDatedTestPayments(user);

        final BigDecimal allTime = this.accountAccessService.getAccountBalance(accountPublicId);
        assertEquals(new BigDecimal("1069.00"), allTime);

        final BigDecimal ranged = this.accountAccessService.getAccountBalance(accountPublicId, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31));
        assertEquals(new BigDecimal("70.00"), ranged);
    }

    @Test
    @TestTransaction
    void getAccountBalances_restrictsToDateRange() {
        final var user = this.userService.createUser(createUniqueUserDto());
        final String username = Objects.requireNonNull(user.username);
        final String accountPublicId = this.createAndAssociateAccount(user);

        this.createTestPayment(username, accountPublicId, null, "200.00", "in range", LocalDate.of(2024, 3, 15));
        this.createTestPayment(username, accountPublicId, null, "500.00", "out of range", LocalDate.of(2024, 6, 1));

        final Map<String, BigDecimal> ranged = this.accountAccessService.getAccountBalances(List.of(accountPublicId),
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31));
        assertEquals(new BigDecimal("200.00"), ranged.get(accountPublicId));

        final Map<String, BigDecimal> allTime = this.accountAccessService.getAccountBalances(List.of(accountPublicId));
        assertEquals(new BigDecimal("700.00"), allTime.get(accountPublicId));
    }

    @Test
    @TestTransaction
    void getAccountPayments_appliesPaginationDateAndQueryFilters() {
        final var user = this.userService.createUser(createUniqueUserDto());
        final String username = Objects.requireNonNull(user.username);
        final String accountPublicId = this.createAndAssociateAccount(user);

        this.createTestPayment(username, accountPublicId, null, "10.00", "groceries", LocalDate.of(2024, 1, 5));
        this.createTestPayment(username, accountPublicId, null, "20.00", "rent payment", LocalDate.of(2024, 1, 15));
        this.createTestPayment(username, accountPublicId, null, "30.00", "old groceries", LocalDate.of(2023, 1, 1));

        // Date range restricts to the two 2024 payments; page size caps the result.
        final List<PaymentResponseDto> firstPage = this.accountAccessService.getAccountPayments(accountPublicId,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), null, 0, 1);
        assertEquals(1, firstPage.size());

        // Free-text query matches against the comment.
        final List<PaymentResponseDto> rentOnly =
                this.accountAccessService.getAccountPayments(accountPublicId, null, null, "rent", 0, 20);
        assertEquals(1, rentOnly.size());
        assertEquals("rent payment", rentOnly.get(0).comment);

        // No filters returns all payments for the account.
        final List<PaymentResponseDto> all =
                this.accountAccessService.getAccountPayments(accountPublicId, null, null, null, 0, 20);
        assertEquals(3, all.size());
    }

    @Test
    @TestTransaction
    void getAccountBalanceHistory_returnsCumulativeZeroStartSeries() {
        final var user = this.userService.createUser(createUniqueUserDto());
        final String accountPublicId = this.createAccountWithDatedTestPayments(user);

        // Restricted to the date range: cumulative starts at zero and only counts in-range payments.
        final List<BalancePointDto> ranged = this.accountAccessService.getAccountBalanceHistory(accountPublicId,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        assertEquals(2, ranged.size());
        assertEquals(LocalDate.of(2024, 1, 10), ranged.get(0).date);
        assertEquals(new BigDecimal("100.00"), ranged.get(0).balance);
        assertEquals(LocalDate.of(2024, 1, 20), ranged.get(1).date);
        assertEquals(new BigDecimal("70.00"), ranged.get(1).balance);

        // All-time: the out-of-range payment becomes the first cumulative point.
        final List<BalancePointDto> allTime =
                this.accountAccessService.getAccountBalanceHistory(accountPublicId, null, null);
        assertEquals(3, allTime.size());
        assertEquals(LocalDate.of(2023, 12, 1), allTime.get(0).date);
        assertEquals(new BigDecimal("999.00"), allTime.get(0).balance);
        assertEquals(new BigDecimal("1099.00"), allTime.get(1).balance);
        assertEquals(new BigDecimal("1069.00"), allTime.get(2).balance);
    }

    @Test
    @TestTransaction
    void getTotalBalanceHistory_appliesIsNegativeSignPerAccount() {
        final var user = this.userService.createUser(createUniqueUserDto());
        final String username = Objects.requireNonNull(user.username);
        final String userPublicId = Objects.requireNonNull(user.publicId);
        final String accountA = this.createAndAssociateAccount(user);
        final String accountB = this.createAndAssociateAccount(user);

        // Account B is flagged negative for this user, so its balance contributes with a flipped sign.
        this.accountAccessService.updateUserAccountMeta(userPublicId, accountB, true, false);

        this.createTestPayment(username, accountA, null, "100.00", "A incoming", LocalDate.of(2024, 2, 1));
        this.createTestPayment(username, accountB, null, "40.00", "B incoming", LocalDate.of(2024, 2, 2));

        final List<BalancePointDto> history = this.accountAccessService.getTotalBalanceHistory(username,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertEquals(2, history.size());
        assertEquals(LocalDate.of(2024, 2, 1), history.get(0).date);
        assertEquals(new BigDecimal("100.00"), history.get(0).balance);
        assertEquals(LocalDate.of(2024, 2, 2), history.get(1).date);
        // 100.00 (A) - 40.00 (B negated) = 60.00
        assertEquals(new BigDecimal("60.00"), history.get(1).balance);
    }

    /** Creates an account associated with the user and seeds it with two in-range and one out-of-range payment. */
    private String createAccountWithDatedTestPayments(final UserResponseDto user) {
        final String username = Objects.requireNonNull(user.username);
        final String accountPublicId = this.createAndAssociateAccount(user);
        this.createTestPayment(username, accountPublicId, null, "100.00", "in-range incoming",
                LocalDate.of(2024, 1, 10));
        this.createTestPayment(username, null, accountPublicId, "30.00", "in-range outgoing",
                LocalDate.of(2024, 1, 20));
        this.createTestPayment(username, accountPublicId, null, "999.00", "out-of-range", LocalDate.of(2023, 12, 1));
        return accountPublicId;
    }

    private void createTestPayment(final String username, @Nullable final String targetAccountPublicId,
            @Nullable final String sourceAccountPublicId, final String amount, final String comment,
            final LocalDate date) {
        final var dto = new PaymentDto();
        dto.targetAccountPublicId = targetAccountPublicId;
        dto.sourceAccountPublicId = sourceAccountPublicId;
        dto.amount = new BigDecimal(amount);
        dto.comment = comment;
        dto.date = date;
        this.paymentService.createPayment(dto, username);
    }

    private String createAndAssociateAccount(final UserResponseDto user) {
        final String userPublicId = Objects.requireNonNull(user.publicId, "user.publicId");
        return this.createAndAssociateAccount(userPublicId);
    }

    private String createAndAssociateAccount(final UserEntity user) {
        final String userPublicId = Objects.requireNonNull(user.publicId, "user.publicId");
        return this.createAndAssociateAccount(userPublicId);
    }

    private String createAndAssociateAccount(final String userPublicId) {
        final AccountDto dto = new AccountDto();
        dto.name = createUniqueTestName("Meta Account");
        final var created = this.accountService.createAccount(dto);

        final String accountPublicId = Objects.requireNonNull(created.publicId, "created.publicId");
        this.accountAccessService.associateUserWithAccount(userPublicId, accountPublicId);
        return accountPublicId;
    }

    @Test
    void getIsNegativeFlagsForUser_returnsEmptyMapForEmptyAccountList() {
        final Map<String, Boolean> flags = this.accountAccessService.getIsNegativeFlagsForUser(1L, List.of());
        assertNotNull(flags);
        assertTrue(flags.isEmpty());
    }

    @Test
    void getPaymentsByAccount_returnsEmptyMapForEmptyAccountList() {
        final Map<String, List<PaymentResponseDto>> result = this.accountAccessService.getPaymentsByAccount(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @TestTransaction
    void getPaymentsByAccount_groupsPaymentsBySourceAndTarget() {
        final var user = this.userService.createUser(createUniqueUserDto());
        final String accountA = this.createAndAssociateAccount(user);
        final String accountB = this.createAndAssociateAccount(user);
        final String username = Objects.requireNonNull(user.username);

        // A -> B: appears under both A (source) and B (target)
        final var transfer = new PaymentDto();
        transfer.sourceAccountPublicId = accountA;
        transfer.targetAccountPublicId = accountB;
        transfer.amount = new BigDecimal("25.00");
        transfer.comment = "A to B";
        transfer.date = LocalDate.now(ZoneId.systemDefault());
        this.paymentService.createPayment(transfer, username);

        // Outgoing-only from A (no target)
        final var outgoing = new PaymentDto();
        outgoing.sourceAccountPublicId = accountA;
        outgoing.amount = new BigDecimal("10.00");
        outgoing.comment = "A outgoing";
        outgoing.date = LocalDate.now(ZoneId.systemDefault());
        this.paymentService.createPayment(outgoing, username);

        // Incoming-only to B (no source)
        final var incoming = new PaymentDto();
        incoming.targetAccountPublicId = accountB;
        incoming.amount = new BigDecimal("7.00");
        incoming.comment = "B incoming";
        incoming.date = LocalDate.now(ZoneId.systemDefault());
        this.paymentService.createPayment(incoming, username);

        // Self transfer on A (source == target): must not be double-counted for A
        final var self = new PaymentDto();
        self.sourceAccountPublicId = accountA;
        self.targetAccountPublicId = accountA;
        self.amount = new BigDecimal("3.00");
        self.comment = "A self";
        self.date = LocalDate.now(ZoneId.systemDefault());
        this.paymentService.createPayment(self, username);

        final Map<String, List<PaymentResponseDto>> result =
                this.accountAccessService.getPaymentsByAccount(List.of(accountA, accountB));

        assertNotNull(result);
        assertEquals(2, result.size());
        // A: transfer + outgoing + self (self counted once)
        assertEquals(3, Objects.requireNonNull(result.get(accountA)).size());
        // B: transfer + incoming
        assertEquals(2, Objects.requireNonNull(result.get(accountB)).size());
    }

    private Long getAccountIdByPublicId(final String publicId) {
        final var account = AccountEntity.find("publicId", publicId).<AccountEntity>firstResult();
        return Objects.requireNonNull(Objects.requireNonNull(account, "Account not found").id);
    }

}
