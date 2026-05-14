package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestDataBuilder.createPaymentDto;
import static de.vptr.midas.api.util.ServiceTestDataBuilder.createUniqueUserDto;
import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.createUniqueTestName;
import static de.vptr.midas.api.util.ServiceTestUtil.generateUniqueTestSuffix;
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.AccountDto;
import de.vptr.midas.api.dto.AccountResponseDto;
import de.vptr.midas.api.util.PaginationUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class AccountServiceTest {
    @Inject
    AccountService accountService;

    @Inject
    UserService userService;

    @Inject
    AccountAccessService accountAccessService;

    @Inject
    PaymentService paymentService;

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.accountService);
    }

    @Test
    void testGetAllAccounts() {
        final var accounts = this.accountService.getAllAccounts();
        assertNotNull(accounts);
    }

    @Test
    @TestTransaction
    void testCreateAccount() {
        final var newAccount = this.createUniqueAccountDto();

        final var createdAccount = this.accountService.createAccount(newAccount);

        assertNotNull(createdAccount);
        assertNotNull(createdAccount.publicId);
        assertEquals(newAccount.name, createdAccount.name);
    }

    @Test
    @TestTransaction
    void testUpdateAccount() {
        // First create an account
        final var newAccount = this.createUniqueAccountDto();
        final var createdAccountDto = this.accountService.createAccount(newAccount);

        final var updateAccount = this.createUniqueAccountDto();

        final var updatedAccountDto = this.accountService
                .updateAccount(Objects.requireNonNull(createdAccountDto.publicId, "publicId"), updateAccount);

        assertNotNull(updatedAccountDto);
        assertEquals(updateAccount.name, updatedAccountDto.name);
    }

    @Test
    @TestTransaction
    void testPatchAccountPreservesNameWhenMissing() {
        final var newAccount = this.createUniqueAccountDto();
        final var createdAccountDto = this.accountService.createAccount(newAccount);
        final var patchDto = new AccountDto();

        final var patchedAccountDto = this.accountService
                .patchAccount(Objects.requireNonNull(createdAccountDto.publicId, "publicId"), patchDto);

        assertNotNull(patchedAccountDto);
        assertEquals(newAccount.name, patchedAccountDto.name);
    }

    @Test
    @TestTransaction
    void testDeleteAccount() {
        // First create an account
        final var newAccount = this.createUniqueAccountDto();
        final var createdAccount = this.accountService.createAccount(newAccount);

        final String accountPublicId = Objects.requireNonNull(createdAccount.publicId, "publicId");

        final var deleted = this.accountService.deleteAccount(accountPublicId);

        assertTrue(deleted);
        final var deletedAccount = this.accountService.findById(accountPublicId);
        assertTrue(deletedAccount.isEmpty());
    }

    @Test
    void testDeleteNonExistentAccount() {
        final var deleted = this.accountService.deleteAccount("999999");
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        // First create an account
        final var newAccount = this.createUniqueAccountDto();
        final var createdAccount = this.accountService.createAccount(newAccount);

        final String createdPublicId = Objects.requireNonNull(createdAccount.publicId, "publicId");
        final var foundAccount = this.accountService.findById(createdPublicId);

        assertTrue(foundAccount.isPresent());
        assertEquals(createdPublicId, foundAccount.get().publicId);
        assertEquals(newAccount.name, foundAccount.get().name);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundAccount = this.accountService.findById("999999");
        assertTrue(foundAccount.isEmpty());
    }

    @Test
    @TestTransaction
    void testSearchAccountsBatchesAssociatedUsersLookup() {
        final String searchToken = "n1-account-search-" + generateUniqueTestSuffix();
        final var firstAccount = this.createAccountDto(searchToken + "-1");
        final var secondAccount = this.createAccountDto(searchToken + "-2");

        final var createdFirst = this.accountService.createAccount(firstAccount);
        final var createdSecond = this.accountService.createAccount(secondAccount);

        final var firstUser = this.userService.createUser(createUniqueUserDto());
        final var secondUser = this.userService.createUser(createUniqueUserDto());

        final String firstUserPublicId = Objects.requireNonNull(firstUser.publicId, "firstUser.publicId");
        final String secondUserPublicId = Objects.requireNonNull(secondUser.publicId, "secondUser.publicId");
        final String createdFirstPublicId = Objects.requireNonNull(createdFirst.publicId, "createdFirst.publicId");
        final String createdSecondPublicId = Objects.requireNonNull(createdSecond.publicId, "createdSecond.publicId");
        this.accountAccessService.associateUserWithAccount(firstUserPublicId, createdFirstPublicId);
        this.accountAccessService.associateUserWithAccount(secondUserPublicId, createdSecondPublicId);

        final var results = this.accountService.searchAccounts(searchToken);
        assertEquals(2, results.size());

        final Map<String, AccountResponseDto> resultsById = results.stream().collect(Collectors
                .toMap(account -> Objects.requireNonNull(account.publicId, "account.publicId"), account -> account));

        assertTrue(resultsById.containsKey(createdFirstPublicId));
        assertTrue(resultsById.containsKey(createdSecondPublicId));
        assertEquals(1, Objects.requireNonNull(resultsById.get(createdFirstPublicId), "result").userCount);
        assertEquals(1, Objects.requireNonNull(resultsById.get(createdSecondPublicId), "result").userCount);
        assertTrue(Objects
                .requireNonNull(
                        Objects.requireNonNull(resultsById.get(createdFirstPublicId), "result").associatedUserPublicIds)
                .contains(firstUserPublicId));
        assertTrue(Objects.requireNonNull(
                Objects.requireNonNull(resultsById.get(createdSecondPublicId), "result").associatedUserPublicIds)
                .contains(secondUserPublicId));
    }

    @Test
    void testSearchAccountsWithBlankQueryUsesDefaultListing() {
        final var expected = this.accountService.getAllAccounts();

        final var results = this.accountService.searchAccounts("   ");

        assertEquals(expected.stream().map(account -> account.publicId).toList(),
                results.stream().map(account -> account.publicId).toList());
    }

    @Test
    @TestTransaction
    void testSearchAccountsEscapesLikeWildcards() {
        final String token = "account-literal-" + generateUniqueTestSuffix();
        final var literalMatch = this.createAccountDto(token + "-%-match");
        final var wildcardOnlyMatch = this.createAccountDto(token + "-plain-match");

        final var createdLiteralMatch = this.accountService.createAccount(literalMatch);
        this.accountService.createAccount(wildcardOnlyMatch);

        final var results = this.accountService.searchAccounts(token + "-%");

        assertEquals(List.of(Objects.requireNonNull(createdLiteralMatch.publicId, "publicId")),
                results.stream().map(account -> account.publicId).toList());
    }

    @Test
    @TestTransaction
    void testSearchAccountsAppliesSharedResultLimit() {
        final String token = "account-limit-" + generateUniqueTestSuffix();
        for (int i = 0; i < PaginationUtil.MAX_SIZE + 5; i++) {
            final var account = this.createUniqueAccountDto();
            account.name = token + "-" + i;
            this.accountService.createAccount(account);
        }

        final var results = this.accountService.searchAccounts(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }

    @Test
    @TestTransaction
    void testPatchAccount_updatesName_whenNameIsNotNull() {
        final var newAccount = this.createUniqueAccountDto();
        final var created = this.accountService.createAccount(newAccount);
        final String publicId = Objects.requireNonNull(created.publicId, "publicId");

        final var patchDto = new AccountDto();
        patchDto.name = "Patched Name " + generateUniqueTestSuffix();

        final var patched = this.accountService.patchAccount(publicId, patchDto);
        assertEquals(patchDto.name, patched.name);
    }

    @Test
    @TestTransaction
    void testDeleteAccount_throwsConflict_whenPaymentsExist() {
        final var source = this.accountService.createAccount(this.createUniqueAccountDto());
        final var target = this.accountService.createAccount(this.createUniqueAccountDto());

        final var paymentDto = createPaymentDto(source.publicId, target.publicId, "test payment",
                LocalDate.now(ZoneId.systemDefault()), BigDecimal.ONE);
        this.paymentService.createPayment(paymentDto, "admin");

        final String sourceId = Objects.requireNonNull(source.publicId, "publicId");
        final var ex = assertThrows(WebApplicationException.class, () -> this.accountService.deleteAccount(sourceId));
        assertEquals(409, ex.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testUpdateAccount_throwsNotFound_whenAccountDoesNotExist() {
        final var updateDto = this.createUniqueAccountDto();
        assertThrows(WebApplicationException.class,
                () -> this.accountService.updateAccount("nonexistent-public-id", updateDto));
    }

    private AccountDto createUniqueAccountDto() {
        return this.createAccountDto(createUniqueTestName("Test Account"));
    }

    private AccountDto createAccountDto(final String name) {
        final var account = new AccountDto();
        account.name = name;
        return account;
    }
}
