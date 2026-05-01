package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.generateUniqueTestSuffix;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestUser;
import static de.vptr.midas.api.util.TestUtil.ADMIN_USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.PaymentEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.AbstractIntegrationTest;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ServiceTestDataBuilder;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class PaymentServiceTest extends AbstractIntegrationTest {
    @Inject
    PaymentService paymentService;

    @Inject
    UserService userService;

    private UserEntity testUser;
    private AccountEntity testSourceAccount;
    private AccountEntity testTargetAccount;

    @BeforeEach
    void setUp() {
        runInIsolatedTransaction(() -> {
            this.testUser = setupTestUser(this.userService);
            this.testSourceAccount = ServiceTestDataBuilder.createUniqueAccountEntity();
            this.testSourceAccount.name = "Source Account";
            this.testSourceAccount.persist();

            this.testTargetAccount = ServiceTestDataBuilder.createUniqueAccountEntity();
            this.testTargetAccount.name = "Target Account";
            this.testTargetAccount.persist();
        });
    }

    @AfterEach
    void tearDown() {
        runCleanup(() -> {
            PaymentEntity.deleteAll();
            if (this.testSourceAccount != null) {
                AccountEntity.deleteById(this.testSourceAccount.id);
            }
            if (this.testTargetAccount != null) {
                AccountEntity.deleteById(this.testTargetAccount.id);
            }
            UserEntity.delete("username like ?1", "testuser_%");
        });
    }

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.paymentService);
    }

    @Test
    void testGetAllPayments() {
        final var payments = this.paymentService.getAllPayments();
        assertNotNull(payments);
    }

    @Test
    @TestTransaction
    void testCreatePayment() {
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Test payment",
                LocalDate.now(),
                new BigDecimal("100.50"));

        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        assertNotNull(createdPayment);
        assertNotNull(createdPayment.id);
        assertEquals(this.testTargetAccount.id, createdPayment.targetAccountId);
        assertEquals(this.testSourceAccount.id, createdPayment.sourceAccountId);
        assertEquals(this.testUser.id, createdPayment.userId);
        assertEquals("Test payment", createdPayment.comment);
        assertEquals(LocalDate.now(), createdPayment.date);
        assertEquals(new BigDecimal("100.50"), createdPayment.amount);
        assertNotNull(createdPayment.created);
    }

    @Test
    @TestTransaction
    void testUpdatePayment() {
        // First create a payment
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Original comment",
                LocalDate.now(),
                new BigDecimal("50.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        // Update the payment
        final var updateDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Updated comment",
                LocalDate.now(),
                new BigDecimal("75.00"));

        final var updatedPayment = this.paymentService.updatePayment(createdPayment.id, updateDto, ADMIN_USERNAME);

        assertNotNull(updatedPayment);
        assertEquals("Updated comment", updatedPayment.comment);
        assertEquals(new BigDecimal("75.00"), updatedPayment.amount);
        assertNotNull(updatedPayment.lastEdit);
    }

    @Test
    @TestTransaction
    void testUpdatePaymentValidationNoAccounts() {
        final var createDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Original comment",
                LocalDate.now(),
                new BigDecimal("50.00"));
        final var createdPayment = this.paymentService.createPayment(createDto);

        final var updateDto = new PaymentDto();
        updateDto.userId = this.testUser.id;
        updateDto.comment = "Updated comment";
        updateDto.date = LocalDate.now();
        updateDto.amount = new BigDecimal("75.00");

        assertThrows(ValidationException.class,
                () -> this.paymentService.updatePayment(createdPayment.id, updateDto, ADMIN_USERNAME));
    }

    @Test
    @TestTransaction
    void testDeletePayment() {
        // First create a payment
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Delete test payment",
                LocalDate.now(),
                new BigDecimal("25.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        final Long paymentId = createdPayment.id;

        final var deleted = this.paymentService.deletePayment(paymentId, ADMIN_USERNAME);

        assertTrue(deleted);
        final var deletedPayment = this.paymentService.findById(paymentId, ADMIN_USERNAME);
        assertTrue(deletedPayment.isEmpty());
    }

    @Test
    void testDeleteNonExistentPayment() {
        final var deleted = this.paymentService.deletePayment(999999L, null);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        // First create a payment
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Find test payment",
                LocalDate.now(),
                new BigDecimal("200.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        final var foundPayment = this.paymentService.findById(createdPayment.id, ADMIN_USERNAME);

        assertTrue(foundPayment.isPresent());
        assertEquals(createdPayment.id, foundPayment.get().id);
        assertEquals("Find test payment", foundPayment.get().comment);
        assertEquals(new BigDecimal("200.00"), foundPayment.get().amount);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundPayment = this.paymentService.findById(999999L, null);
        assertTrue(foundPayment.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByUserId() {
        // First create a payment
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Payment test",
                LocalDate.now(),
                new BigDecimal("150.00"));
        this.paymentService.createPayment(newPaymentDto);

        final var userPayments = this.paymentService.findByUserId(this.testUser.id);

        assertNotNull(userPayments);
        assertFalse(userPayments.isEmpty());

        // Verify all payments belong to the user
        for (final var payment : userPayments) {
            assertEquals(this.testUser.id, payment.userId);
        }
    }

    @Test
    @TestTransaction
    void testFindByDateRange() {
        final LocalDate startDate = LocalDate.now().minusDays(1);
        final LocalDate endDate = LocalDate.now().plusDays(1);

        // First create a payment within the date range
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Date range test payment",
                LocalDate.now(),
                new BigDecimal("100.00"));
        this.paymentService.createPayment(newPaymentDto);

        final var paymentsInRange = this.paymentService.findByDateRange(startDate, endDate);

        assertNotNull(paymentsInRange);
        assertFalse(paymentsInRange.isEmpty());

        // Verify all payments are within the date range
        for (final var payment : paymentsInRange) {
            assertFalse(payment.date.isBefore(startDate));
            assertFalse(payment.date.isAfter(endDate));
        }
    }

    @Test
    @TestTransaction
    void testGetTotalAmountByUser() {
        final var firstPayment = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "First payment",
                LocalDate.now(),
                new BigDecimal("100.50"));
        final var secondPayment = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Second payment",
                LocalDate.now(),
                new BigDecimal("49.50"));

        this.paymentService.createPayment(firstPayment);
        this.paymentService.createPayment(secondPayment);

        final BigDecimal total = this.paymentService.getTotalAmountByUser(this.testUser.id);

        assertEquals(new BigDecimal("150.00"), total);
    }

    @Test
    @TestTransaction
    void testFindBySourceAccountId() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id, this.testTargetAccount.id,
                this.testUser.id,
                "Source test", LocalDate.now(), new BigDecimal("100.00"));
        this.paymentService.createPayment(dto);

        final var payments = this.paymentService.findBySourceAccountId(this.testSourceAccount.id);
        assertNotNull(payments);
        assertFalse(payments.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByTargetAccountId() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id, this.testTargetAccount.id,
                this.testUser.id,
                "Target test", LocalDate.now(), new BigDecimal("100.00"));
        this.paymentService.createPayment(dto);

        final var payments = this.paymentService.findByTargetAccountId(this.testTargetAccount.id);
        assertNotNull(payments);
        assertFalse(payments.isEmpty());
    }

    @Test
    @TestTransaction
    void testSearchPaymentsWithQuery() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id, this.testTargetAccount.id,
                this.testUser.id,
                "UniqueSearchComment123", LocalDate.now(), new BigDecimal("50.00"));
        this.paymentService.createPayment(dto);

        final var results = this.paymentService.searchPayments("UniqueSearchComment123");
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    void testSearchPaymentsWithNull() {
        final var expected = this.paymentService.getAllPayments();

        final var results = this.paymentService.searchPayments(null);

        assertEquals(expected.stream().map(payment -> payment.id).toList(),
                results.stream().map(payment -> payment.id).toList());
    }

    @Test
    void testSearchPaymentsWithBlankQueryUsesDefaultListing() {
        final var expected = this.paymentService.getAllPayments();

        final var results = this.paymentService.searchPayments("   ");

        assertEquals(expected.stream().map(payment -> payment.id).toList(),
                results.stream().map(payment -> payment.id).toList());
    }

    @Test
    @TestTransaction
    void testSearchPaymentsEscapesLikeWildcards() {
        final String token = "payment-search-" + generateUniqueTestSuffix();
        final var literalMatch = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id,
                this.testTargetAccount.id, this.testUser.id,
                token + "_literal", LocalDate.now(), new BigDecimal("11.00"));
        final var wildcardOnlyMatch = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id,
                this.testTargetAccount.id, this.testUser.id,
                token + "xliteral", LocalDate.now(), new BigDecimal("12.00"));

        final var createdLiteralMatch = this.paymentService.createPayment(literalMatch);
        this.paymentService.createPayment(wildcardOnlyMatch);

        final var results = this.paymentService.searchPayments(token + "_");

        assertEquals(List.of(createdLiteralMatch.id), results.stream().map(payment -> payment.id).toList());
    }

    @Test
    @TestTransaction
    void testSearchPaymentsAppliesSharedResultLimit() {
        final String token = "payment-limit-" + generateUniqueTestSuffix();
        for (int i = 0; i < PaginationUtil.MAX_SIZE + 5; i++) {
            final var payment = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id,
                    this.testTargetAccount.id, this.testUser.id,
                    token + "-" + i, LocalDate.now().minusDays(i % 5L), BigDecimal.valueOf(i + 1L));
            this.paymentService.createPayment(payment);
        }

        final var results = this.paymentService.searchPayments(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }

    @Test
    @TestTransaction
    void testPatchPaymentComment() {
        final var createDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id,
                this.testTargetAccount.id, this.testUser.id,
                "Original", LocalDate.now(), new BigDecimal("100.00"));
        final var created = this.paymentService.createPayment(createDto);

        final var patchDto = new PaymentDto();
        patchDto.comment = "Patched comment";

        final var patched = this.paymentService.patchPayment(created.id, patchDto, ADMIN_USERNAME);
        assertNotNull(patched);
        assertEquals("Patched comment", patched.comment);
        assertEquals(new BigDecimal("100.00"), patched.amount);
    }

    @Test
    @TestTransaction
    void testPatchPaymentAmount() {
        final var createDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id,
                this.testTargetAccount.id, this.testUser.id,
                "Amount test", LocalDate.now(), new BigDecimal("100.00"));
        final var created = this.paymentService.createPayment(createDto);

        final var patchDto = new PaymentDto();
        patchDto.amount = new BigDecimal("200.00");

        final var patched = this.paymentService.patchPayment(created.id, patchDto, ADMIN_USERNAME);
        assertNotNull(patched);
        assertEquals(new BigDecimal("200.00"), patched.amount);
    }

    @Test
    @TestTransaction
    void testFindByIdDeniedWhenUsernameMissing() {
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(
                this.testSourceAccount.id,
                this.testTargetAccount.id,
                this.testUser.id,
                "Forbidden lookup",
                LocalDate.now(),
                new BigDecimal("10.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        final var exception = assertThrows(WebApplicationException.class,
                () -> this.paymentService.findById(createdPayment.id, null));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testFindByIdDeniedWhenPaymentHasNoAccounts() {
        final PaymentEntity malformedPayment = new PaymentEntity();
        malformedPayment.user = this.testUser;
        malformedPayment.comment = "Malformed payment";
        malformedPayment.date = LocalDate.now();
        malformedPayment.amount = new BigDecimal("10.00");
        malformedPayment.persist();

        final var exception = assertThrows(WebApplicationException.class,
                () -> this.paymentService.findById(malformedPayment.id, ADMIN_USERNAME));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testPatchPaymentDeniedWhenPaymentHasNoAccounts() {
        final PaymentEntity malformedPayment = new PaymentEntity();
        malformedPayment.user = this.testUser;
        malformedPayment.comment = "Malformed payment";
        malformedPayment.date = LocalDate.now();
        malformedPayment.amount = new BigDecimal("10.00");
        malformedPayment.persist();

        final PaymentDto patchDto = new PaymentDto();
        patchDto.comment = "Still malformed";

        final var exception = assertThrows(WebApplicationException.class,
                () -> this.paymentService.patchPayment(malformedPayment.id, patchDto, ADMIN_USERNAME));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    void testPatchPaymentNotFound() {
        final var patchDto = new PaymentDto();
        patchDto.comment = "test";
        assertThrows(WebApplicationException.class, () -> this.paymentService.patchPayment(999999L, patchDto, null));
    }

    @Test
    @TestTransaction
    void testFindRecentPayments() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id, this.testTargetAccount.id,
                this.testUser.id,
                "Recent", LocalDate.now(), new BigDecimal("10.00"));
        this.paymentService.createPayment(dto);

        final var recent = this.paymentService.findRecentPayments(5);
        assertNotNull(recent);
        assertFalse(recent.isEmpty());
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoAccounts() {
        final var dto = new PaymentDto();
        dto.userId = this.testUser.id;
        dto.comment = "test";
        dto.date = LocalDate.now();
        dto.amount = new BigDecimal("10.00");
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoUser() {
        final var dto = new PaymentDto();
        dto.sourceAccountId = this.testSourceAccount.id;
        dto.targetAccountId = this.testTargetAccount.id;
        dto.comment = "test";
        dto.date = LocalDate.now();
        dto.amount = new BigDecimal("10.00");
        assertThrows(IllegalArgumentException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoComment() {
        final var dto = new PaymentDto();
        dto.sourceAccountId = this.testSourceAccount.id;
        dto.targetAccountId = this.testTargetAccount.id;
        dto.userId = this.testUser.id;
        dto.date = LocalDate.now();
        dto.amount = new BigDecimal("10.00");
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoDate() {
        final var dto = new PaymentDto();
        dto.sourceAccountId = this.testSourceAccount.id;
        dto.targetAccountId = this.testTargetAccount.id;
        dto.userId = this.testUser.id;
        dto.comment = "test";
        dto.amount = new BigDecimal("10.00");
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentWithZeroAmount() {
        final var dto = new PaymentDto();
        dto.sourceAccountId = this.testSourceAccount.id;
        dto.targetAccountId = this.testTargetAccount.id;
        dto.userId = this.testUser.id;
        dto.comment = "test";
        dto.date = LocalDate.now();
        dto.amount = BigDecimal.ZERO;
        final var created = this.paymentService.createPayment(dto);
        assertNotNull(created);
        assertEquals(BigDecimal.ZERO.compareTo(created.amount), 0);
    }

    @Test
    @TestTransaction
    void testCreatePaymentWithOnlySourceAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id, null, this.testUser.id,
                "Source only",
                LocalDate.now(), new BigDecimal("10.00"));
        final var created = this.paymentService.createPayment(dto);
        assertNotNull(created);
        assertNotNull(created.sourceAccountId);
    }

    @Test
    @TestTransaction
    void testCreatePaymentWithOnlyTargetAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(null, this.testTargetAccount.id, this.testUser.id,
                "Target only",
                LocalDate.now(), new BigDecimal("10.00"));
        final var created = this.paymentService.createPayment(dto);
        assertNotNull(created);
        assertNotNull(created.targetAccountId);
    }

    @Test
    @TestTransaction
    void testCreatePaymentInvalidTargetAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id, 999999L, this.testUser.id,
                "Bad target",
                LocalDate.now(), new BigDecimal("10.00"));
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentInvalidSourceAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(999999L, this.testTargetAccount.id, this.testUser.id,
                "Bad source",
                LocalDate.now(), new BigDecimal("10.00"));
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentInvalidUser() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccount.id, this.testTargetAccount.id,
                999999L,
                "Bad user", LocalDate.now(), new BigDecimal("10.00"));
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    void testGetAllPaymentsWithPagination() {
        final var payments = this.paymentService.getAllPayments(0, 5);
        assertNotNull(payments);
    }
}
