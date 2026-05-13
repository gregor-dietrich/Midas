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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

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
@SuppressWarnings("PMD.TooManyMethods")
class PaymentServiceTest extends AbstractIntegrationTest {
    @Inject
    PaymentService paymentService;

    @Inject
    AccountAccessService accountAccessService;

    @Inject
    UserService userService;

    private UserEntity testUser;
    private AccountEntity testSourceAccount;
    private AccountEntity testTargetAccount;

    private String testUserPublicId;
    private String testSourceAccountPublicId;
    private String testTargetAccountPublicId;

    private long testSourceAccountId;
    private long testTargetAccountId;

    @SuppressWarnings("NullAway")
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

            this.testUserPublicId = Objects.requireNonNull(this.testUser.publicId);
            this.testSourceAccountPublicId = Objects.requireNonNull(this.testSourceAccount.publicId);
            this.testTargetAccountPublicId = Objects.requireNonNull(this.testTargetAccount.publicId);

            this.testSourceAccountId = Objects.requireNonNull(this.testSourceAccount.id);
            this.testTargetAccountId = Objects.requireNonNull(this.testTargetAccount.id);
        });
    }

    @AfterEach
    void tearDown() {
        runCleanup(() -> {
            PaymentEntity.deleteAll();
            if (this.testSourceAccountId != 0L) {
                AccountEntity.deleteById(this.testSourceAccountId);
            }
            if (this.testTargetAccountId != 0L) {
                AccountEntity.deleteById(this.testTargetAccountId);
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
        final var newPaymentDto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "Test payment", LocalDate.now(ZoneOffset.UTC), new BigDecimal("100.50"));

        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        assertNotNull(createdPayment);
        assertNotNull(createdPayment.publicId);
        assertEquals(this.testTargetAccountPublicId, createdPayment.targetAccountPublicId);
        assertEquals(this.testSourceAccountPublicId, createdPayment.sourceAccountPublicId);
        assertEquals(this.testUserPublicId, createdPayment.userPublicId);
        assertEquals("Test payment", createdPayment.comment);
        assertEquals(LocalDate.now(ZoneOffset.UTC), createdPayment.date);
        assertEquals(new BigDecimal("100.50"), createdPayment.amount);
        assertNotNull(createdPayment.created);
    }

    @Test
    @TestTransaction
    void testUpdatePayment() {
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Original comment",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("50.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        final var updateDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Updated comment", LocalDate.now(ZoneOffset.UTC),
                new BigDecimal("75.00"));

        final var updatedPayment = this.paymentService.updatePayment(Objects.requireNonNull(createdPayment.publicId),
                updateDto, ADMIN_USERNAME);

        assertNotNull(updatedPayment);
        assertEquals("Updated comment", updatedPayment.comment);
        assertEquals(new BigDecimal("75.00"), updatedPayment.amount);
        assertNotNull(updatedPayment.lastEdit);
    }

    @Test
    @TestTransaction
    void testUpdatePaymentValidationNoAccounts() {
        final var createDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Original comment",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("50.00"));
        final var createdPayment = this.paymentService.createPayment(createDto);

        final var updateDto = new PaymentDto();
        updateDto.userPublicId = this.testUserPublicId;
        updateDto.comment = "Updated comment";
        updateDto.date = LocalDate.now(ZoneOffset.UTC);
        updateDto.amount = new BigDecimal("75.00");

        assertThrows(ValidationException.class, () -> this.paymentService
                .updatePayment(Objects.requireNonNull(createdPayment.publicId), updateDto, ADMIN_USERNAME));
    }

    @Test
    @TestTransaction
    void testDeletePayment() {
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Delete test payment",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("25.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        final String paymentPublicId = Objects.requireNonNull(createdPayment.publicId);

        final var deleted = this.paymentService.deletePayment(paymentPublicId, ADMIN_USERNAME);

        assertTrue(deleted);
        final var deletedPayment = this.paymentService.findById(paymentPublicId, ADMIN_USERNAME);
        assertTrue(deletedPayment.isEmpty());
    }

    @Test
    void testDeleteNonExistentPayment() {
        final var deleted = this.paymentService.deletePayment("999999", null);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Find test payment",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("200.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        final String createdPublicId = Objects.requireNonNull(createdPayment.publicId);
        final var foundPayment = this.paymentService.findById(createdPublicId, ADMIN_USERNAME);

        assertTrue(foundPayment.isPresent());
        assertEquals(createdPublicId, foundPayment.get().publicId);
        assertEquals("Find test payment", foundPayment.get().comment);
        assertEquals(new BigDecimal("200.00"), foundPayment.get().amount);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundPayment = this.paymentService.findById("999999", null);
        assertTrue(foundPayment.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByUserId() {
        final var newPaymentDto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "Payment test", LocalDate.now(ZoneOffset.UTC), new BigDecimal("150.00"));
        this.paymentService.createPayment(newPaymentDto);

        final var userPayments = this.paymentService.findByUserPublicId(this.testUserPublicId);

        assertNotNull(userPayments);
        assertFalse(userPayments.isEmpty());

        for (final var payment : userPayments) {
            assertEquals(this.testUserPublicId, payment.userPublicId);
        }
    }

    @Test
    @TestTransaction
    void testFindByDateRange() {
        final LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        final LocalDate endDate = LocalDate.now(ZoneOffset.UTC).plusDays(1);

        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Date range test payment",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("100.00"));
        this.paymentService.createPayment(newPaymentDto);

        final var paymentsInRange = this.paymentService.findByDateRange(startDate, endDate);

        assertNotNull(paymentsInRange);
        assertFalse(paymentsInRange.isEmpty());

        for (final var payment : paymentsInRange) {
            assertFalse(Objects.requireNonNull(payment.date).isBefore(startDate));
            assertFalse(Objects.requireNonNull(payment.date).isAfter(endDate));
        }
    }

    @Test
    @TestTransaction
    void testGetTotalAmountByUser() {
        final var firstPayment = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "First payment", LocalDate.now(ZoneOffset.UTC),
                new BigDecimal("100.50"));
        final var secondPayment = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Second payment", LocalDate.now(ZoneOffset.UTC),
                new BigDecimal("49.50"));

        this.paymentService.createPayment(firstPayment);
        this.paymentService.createPayment(secondPayment);

        final BigDecimal total = this.paymentService.getTotalAmountByUserPublicId(this.testUserPublicId);

        assertEquals(new BigDecimal("150.00"), total);
    }

    @Test
    @TestTransaction
    void testFindBySourceAccountId() {
        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "Source test", LocalDate.now(ZoneOffset.UTC), new BigDecimal("100.00"));
        this.paymentService.createPayment(dto);

        final var payments = this.paymentService.findBySourceAccountPublicId(this.testSourceAccountPublicId);
        assertNotNull(payments);
        assertFalse(payments.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByTargetAccountId() {
        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "Target test", LocalDate.now(ZoneOffset.UTC), new BigDecimal("100.00"));
        this.paymentService.createPayment(dto);

        final var payments = this.paymentService.findByTargetAccountPublicId(this.testTargetAccountPublicId);
        assertNotNull(payments);
        assertFalse(payments.isEmpty());
    }

    @Test
    @TestTransaction
    void testSearchPaymentsWithQuery() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "UniqueSearchComment123",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("50.00"));
        this.paymentService.createPayment(dto);

        final var results = this.paymentService.searchPayments("UniqueSearchComment123");
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    void testSearchPaymentsWithNull() {
        final var expected = this.paymentService.getAllPayments();

        final var results = this.paymentService.searchPayments(null);

        assertEquals(expected.stream().map(payment -> payment.publicId).toList(),
                results.stream().map(payment -> payment.publicId).toList());
    }

    @Test
    void testSearchPaymentsWithBlankQueryUsesDefaultListing() {
        final var expected = this.paymentService.getAllPayments();

        final var results = this.paymentService.searchPayments("   ");

        assertEquals(expected.stream().map(payment -> payment.publicId).toList(),
                results.stream().map(payment -> payment.publicId).toList());
    }

    @Test
    @TestTransaction
    void testSearchPaymentsEscapesLikeWildcards() {
        final String token = "payment-search-" + generateUniqueTestSuffix();
        final var literalMatch = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, token + "_literal",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("11.00"));
        final var wildcardOnlyMatch = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, token + "xliteral",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("12.00"));

        final var createdLiteralMatch = this.paymentService.createPayment(literalMatch);
        this.paymentService.createPayment(wildcardOnlyMatch);

        final var results = this.paymentService.searchPayments(token + "_");

        assertEquals(List.of(Objects.requireNonNull(createdLiteralMatch.publicId)),
                results.stream().map(payment -> payment.publicId).toList());
    }

    @Test
    @TestTransaction
    void testSearchPaymentsAppliesSharedResultLimit() {
        final String token = "payment-limit-" + generateUniqueTestSuffix();
        for (int i = 0; i < PaginationUtil.MAX_SIZE + 5; i++) {
            final var payment = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                    this.testTargetAccountPublicId, this.testUserPublicId, token + "-" + i,
                    LocalDate.now(ZoneOffset.UTC).minusDays(i % 5L), BigDecimal.valueOf(i + 1L));
            this.paymentService.createPayment(payment);
        }

        final var results = this.paymentService.searchPayments(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }

    @Test
    @TestTransaction
    void testPatchPaymentComment() {
        final var createDto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "Original", LocalDate.now(ZoneOffset.UTC), new BigDecimal("100.00"));
        final var created = this.paymentService.createPayment(createDto);

        final var patchDto = new PaymentDto();
        patchDto.comment = "Patched comment";

        final var patched =
                this.paymentService.patchPayment(Objects.requireNonNull(created.publicId), patchDto, ADMIN_USERNAME);
        assertNotNull(patched);
        assertEquals("Patched comment", patched.comment);
        assertEquals(new BigDecimal("100.00"), patched.amount);
    }

    @Test
    @TestTransaction
    void testPatchPaymentAmount() {
        final var createDto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "Amount test", LocalDate.now(ZoneOffset.UTC), new BigDecimal("100.00"));
        final var created = this.paymentService.createPayment(createDto);

        final var patchDto = new PaymentDto();
        patchDto.amount = new BigDecimal("200.00");

        final var patched =
                this.paymentService.patchPayment(Objects.requireNonNull(created.publicId), patchDto, ADMIN_USERNAME);
        assertNotNull(patched);
        assertEquals(new BigDecimal("200.00"), patched.amount);
    }

    @Test
    @TestTransaction
    void testFindByIdDeniedWhenUsernameMissing() {
        final var newPaymentDto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "Forbidden lookup",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("10.00"));
        final var createdPayment = this.paymentService.createPayment(newPaymentDto);

        final String createdPublicId = Objects.requireNonNull(createdPayment.publicId);
        final var exception =
                assertThrows(WebApplicationException.class, () -> this.paymentService.findById(createdPublicId, null));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testFindByIdDeniedWhenPaymentHasNoAccounts() {
        final PaymentEntity malformedPayment = new PaymentEntity();
        malformedPayment.user = this.testUser;
        malformedPayment.comment = "Malformed payment";
        malformedPayment.date = LocalDate.now(ZoneOffset.UTC);
        malformedPayment.amount = new BigDecimal("10.00");
        malformedPayment.persist();

        final String malformedPublicId = Objects.requireNonNull(malformedPayment.publicId);
        final var exception = assertThrows(WebApplicationException.class,
                () -> this.paymentService.findById(malformedPublicId, ADMIN_USERNAME));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testPatchPaymentDeniedWhenPaymentHasNoAccounts() {
        final PaymentEntity malformedPayment = new PaymentEntity();
        malformedPayment.user = this.testUser;
        malformedPayment.comment = "Malformed payment";
        malformedPayment.date = LocalDate.now(ZoneOffset.UTC);
        malformedPayment.amount = new BigDecimal("10.00");
        malformedPayment.persist();

        final PaymentDto patchDto = new PaymentDto();
        patchDto.comment = "Still malformed";

        final String malformedPublicId = Objects.requireNonNull(malformedPayment.publicId);
        final var exception = assertThrows(WebApplicationException.class,
                () -> this.paymentService.patchPayment(malformedPublicId, patchDto, ADMIN_USERNAME));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    void testPatchPaymentNotFound() {
        final var patchDto = new PaymentDto();
        patchDto.comment = "test";
        assertThrows(WebApplicationException.class, () -> this.paymentService.patchPayment("999999", patchDto, null));
    }

    @Test
    @TestTransaction
    void testFindRecentPayments() {
        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "Recent", LocalDate.now(ZoneOffset.UTC), new BigDecimal("10.00"));
        this.paymentService.createPayment(dto);

        final var recent = this.paymentService.findRecentPayments(5);
        assertNotNull(recent);
        assertFalse(recent.isEmpty());
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoAccounts() {
        final var dto = new PaymentDto();
        dto.userPublicId = this.testUserPublicId;
        dto.comment = "test";
        dto.date = LocalDate.now(ZoneOffset.UTC);
        dto.amount = new BigDecimal("10.00");
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoUser() {
        final var dto = new PaymentDto();
        dto.sourceAccountPublicId = this.testSourceAccountPublicId;
        dto.targetAccountPublicId = this.testTargetAccountPublicId;
        dto.comment = "test";
        dto.date = LocalDate.now(ZoneOffset.UTC);
        dto.amount = new BigDecimal("10.00");
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoComment() {
        final var dto = new PaymentDto();
        dto.sourceAccountPublicId = this.testSourceAccountPublicId;
        dto.targetAccountPublicId = this.testTargetAccountPublicId;
        dto.userPublicId = this.testUserPublicId;
        dto.date = LocalDate.now(ZoneOffset.UTC);
        dto.amount = new BigDecimal("10.00");
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentValidationNoDate() {
        final var dto = new PaymentDto();
        dto.sourceAccountPublicId = this.testSourceAccountPublicId;
        dto.targetAccountPublicId = this.testTargetAccountPublicId;
        dto.userPublicId = this.testUserPublicId;
        dto.comment = "test";
        dto.amount = new BigDecimal("10.00");
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentWithZeroAmount() {
        final var dto = new PaymentDto();
        dto.sourceAccountPublicId = this.testSourceAccountPublicId;
        dto.targetAccountPublicId = this.testTargetAccountPublicId;
        dto.userPublicId = this.testUserPublicId;
        dto.comment = "test";
        dto.date = LocalDate.now(ZoneOffset.UTC);
        dto.amount = BigDecimal.ZERO;
        final var created = this.paymentService.createPayment(dto);
        assertNotNull(created);
        assertEquals(BigDecimal.ZERO.compareTo(created.amount), 0);
    }

    @Test
    @TestTransaction
    void testCreatePaymentWithOnlySourceAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, null,
                this.testUserPublicId, "Source only", LocalDate.now(ZoneOffset.UTC), new BigDecimal("10.00"));
        final var created = this.paymentService.createPayment(dto);
        assertNotNull(created);
        assertNotNull(created.sourceAccountPublicId);
    }

    @Test
    @TestTransaction
    void testCreatePaymentWithOnlyTargetAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(null, this.testTargetAccountPublicId,
                this.testUserPublicId, "Target only", LocalDate.now(ZoneOffset.UTC), new BigDecimal("10.00"));
        final var created = this.paymentService.createPayment(dto);
        assertNotNull(created);
        assertNotNull(created.targetAccountPublicId);
    }

    @Test
    @TestTransaction
    void testCreatePaymentInvalidTargetAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, "999999",
                this.testUserPublicId, "Bad target", LocalDate.now(ZoneOffset.UTC), new BigDecimal("10.00"));
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentInvalidSourceAccount() {
        final var dto = ServiceTestDataBuilder.createPaymentDto("999999", this.testTargetAccountPublicId,
                this.testUserPublicId, "Bad source", LocalDate.now(ZoneOffset.UTC), new BigDecimal("10.00"));
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    @TestTransaction
    void testCreatePaymentInvalidUser() {
        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        "999999", "Bad user", LocalDate.now(ZoneOffset.UTC), new BigDecimal("10.00"));
        assertThrows(ValidationException.class, () -> this.paymentService.createPayment(dto));
    }

    @Test
    void testGetAllPaymentsWithPagination() {
        final var payments = this.paymentService.getAllPayments(0, 5);
        assertNotNull(payments);
    }

    @Test
    @TestTransaction
    void testUpdatePaymentAdvanced() {
        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "InitialUpdate", LocalDate.now(ZoneOffset.UTC), new BigDecimal("12.50"));
        final var created = this.paymentService.createPayment(dto);
        final String createdId = Objects.requireNonNull(created.publicId);

        dto.comment = "Updated Comment";
        final var updated = this.paymentService.updatePayment(createdId, dto, ADMIN_USERNAME);
        assertEquals("Updated Comment", updated.comment);

        assertThrows(WebApplicationException.class,
                () -> this.paymentService.updatePayment("999999", dto, ADMIN_USERNAME));
        assertThrows(WebApplicationException.class,
                () -> this.paymentService.updatePayment(createdId, dto, "unauthorized_user"));
    }

    @Test
    @TestTransaction
    void testPatchPaymentAdvanced() {
        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "InitialPatch", LocalDate.now(ZoneOffset.UTC), new BigDecimal("15.00"));
        final var created = this.paymentService.createPayment(dto);
        final String createdId = Objects.requireNonNull(created.publicId);

        final var patchDto = new PaymentDto();
        patchDto.comment = "Patched Comment";
        patchDto.amount = new BigDecimal("25.00");
        final var patched = this.paymentService.patchPayment(createdId, patchDto, ADMIN_USERNAME);
        assertEquals("Patched Comment", patched.comment);
        assertEquals(new BigDecimal("25.00"), patched.amount);

        patchDto.userPublicId = "different_user";
        assertThrows(ValidationException.class,
                () -> this.paymentService.patchPayment(createdId, patchDto, ADMIN_USERNAME));

        final var invalidPatch = new PaymentDto();
        invalidPatch.comment = "   ";
        assertThrows(ValidationException.class,
                () -> this.paymentService.patchPayment(createdId, invalidPatch, ADMIN_USERNAME));

        final var invalidAmountPatch = new PaymentDto();
        invalidAmountPatch.amount = BigDecimal.ZERO;
        assertThrows(ValidationException.class,
                () -> this.paymentService.patchPayment(createdId, invalidAmountPatch, ADMIN_USERNAME));
    }

    @Test
    @TestTransaction
    void testDeletePaymentAdvanced() {
        assertFalse(this.paymentService.deletePayment("999999", ADMIN_USERNAME));

        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "ToDelete", LocalDate.now(ZoneOffset.UTC), new BigDecimal("18.00"));
        final var created = this.paymentService.createPayment(dto);
        final String createdId = Objects.requireNonNull(created.publicId);

        assertTrue(this.paymentService.deletePayment(createdId, ADMIN_USERNAME));
    }

    @Test
    @TestTransaction
    void testSearchPaymentsAdvanced() {
        final var dto = ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId,
                this.testTargetAccountPublicId, this.testUserPublicId, "UniqueSearchComment123",
                LocalDate.now(ZoneOffset.UTC), new BigDecimal("20.00"));
        this.paymentService.createPayment(dto);

        assertFalse(this.paymentService.searchPayments("UniqueSearchComment123").isEmpty());
        assertFalse(this.paymentService.searchPayments(null).isEmpty());
        assertFalse(this.paymentService.searchPayments("   ").isEmpty());
    }

    @Test
    @TestTransaction
    void testPaymentForUserScopedOperations() {
        final var dto =
                ServiceTestDataBuilder.createPaymentDto(this.testSourceAccountPublicId, this.testTargetAccountPublicId,
                        this.testUserPublicId, "UserScoped", LocalDate.now(ZoneOffset.UTC), new BigDecimal("30.00"));
        final String username = Objects.requireNonNull(this.testUser.username);

        assertThrows(WebApplicationException.class,
                () -> this.paymentService.createPaymentForUser(dto, username, this.testSourceAccountPublicId));

        this.accountAccessService.associateUserWithAccount(this.testUserPublicId, this.testSourceAccountPublicId);
        this.accountAccessService.associateUserWithAccount(this.testUserPublicId, this.testTargetAccountPublicId);

        final var created = this.paymentService.createPaymentForUser(dto, username, this.testSourceAccountPublicId);
        assertNotNull(created);
        final String createdId = Objects.requireNonNull(created.publicId);

        dto.comment = "Scoped Update";
        final var updated =
                this.paymentService.updatePaymentForUser(createdId, dto, username, this.testSourceAccountPublicId);
        assertEquals("Scoped Update", updated.comment);

        assertThrows(WebApplicationException.class,
                () -> this.paymentService.updatePaymentForUser(createdId, dto, username, "999999"));
    }
}
