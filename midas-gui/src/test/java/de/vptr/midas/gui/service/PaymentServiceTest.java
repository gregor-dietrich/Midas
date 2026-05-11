package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PaymentClient;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestDataBuilder;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @RestClient
    @Inject
    @Mock
    PaymentClient paymentClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void getAllPayments_shouldReturnPaymentList_whenAuthenticated() {
        final var payment1 = MockDataUtil.createPaymentResponseDto(1L, "testuser", 1L, "Source Account 1", 2L,
                "Target Account 1", new BigDecimal("100.00"), "Payment 1");
        final var payment2 = MockDataUtil.createPaymentResponseDto(2L, "testuser", 2L, "Source Account 2", 1L,
                "Target Account 2", new BigDecimal("50.00"), "Payment 2");
        final var expectedPayments = Arrays.asList(payment1, payment2);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getAllPayments(0, 50, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPayments);

        final var result = this.paymentService.getAllPayments(0, 50);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(payment1, payment2);
        verify(this.paymentClient).getAllPayments(0, 50, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getAllPayments_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService.getAllPayments(0, 50));
    }

    @Test
    void getAllPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getAllPayments(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.paymentService.getAllPayments(0, 50));
    }

    @Test
    void getAllPayments_shouldThrowAuthenticationException_when401Error() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getAllPayments(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.paymentService.getAllPayments(0, 50), this.authService);
    }

    @Test
    void getPaymentById_shouldReturnPayment_whenPaymentExists() {
        final var paymentId = 1L;
        final var expectedPayment = MockDataUtil.createPaymentResponseDto();
        expectedPayment.setId(paymentId);
        expectedPayment.setComment("Test Payment");
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedPayment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPayment(paymentId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.paymentService.getPaymentById(paymentId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(paymentId);
        assertThat(result.get().getComment()).isEqualTo("Test Payment");
    }

    @Test
    void getPaymentById_shouldReturnEmpty_whenPaymentNotFound() {
        final var paymentId = 999L;

        ServiceTestUtil.setupAuthHeader(this.authService);
        ServiceTestUtil.testReturnEmptyOn404(
                () -> when(this.paymentClient.getPayment(paymentId, TestUtil.TEST_AUTH_HEADER))
                        .thenReturn(ResponseTestUtil.createNotFoundResponse()),
                () -> this.paymentService.getPaymentById(paymentId));
    }

    @Test
    void getPaymentsByUser_shouldReturnUserPayments_whenUserExists() {
        final var userId = 1L;
        final var payment = MockDataUtil.createPaymentResponseDto(1L, "testuser", 1L, "Source Account", 2L,
                "Target Account", new BigDecimal("100.00"), "Some Payment");
        final var expectedPayments = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsByUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPayments);

        final var result = this.paymentService.getPaymentsByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComment()).isEqualTo("Some Payment");
    }

    @Test
    void getPaymentsByUser_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService.getPaymentsByUser(1L));
    }

    @Test
    void getRecentPayments_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService.getRecentPayments(5));
    }

    @Test
    void getRecentPayments_shouldReturnRecentPayments_whenLimitProvided() {
        final int limit = 5;
        final var recentPayment = MockDataUtil.createPaymentResponseDto(1L, "testuser", 1L, "Source Account", 2L,
                "Target Account", new BigDecimal("100.00"), "Recent Payment");
        final var expectedPayments = Collections.singletonList(recentPayment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getRecentPayments(limit, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPayments);

        final var result = this.paymentService.getRecentPayments(limit);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComment()).isEqualTo("Recent Payment");
        verify(this.paymentClient).getRecentPayments(limit, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getPaymentsByDateRange_shouldReturnPaymentsInRange_whenValidDates() {
        final LocalDate startDate = LocalDate.of(2023, 1, 1);
        final LocalDate endDate = LocalDate.of(2023, 12, 31);
        final var payment = MockDataUtil.createPaymentResponseDto(1L, "testuser", 1L, "Source Account", 2L,
                "Target Account", new BigDecimal("100.00"), "Date Range Payment");
        payment.setDate(LocalDate.of(2023, 6, 15));
        final var expectedPayments = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsByDateRange(startDate, endDate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(expectedPayments);

        final var result = this.paymentService.getPaymentsByDateRange(startDate, endDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComment()).isEqualTo("Date Range Payment");
        verify(this.paymentClient).getPaymentsByDateRange(startDate, endDate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getPaymentsByDateRange_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService
                .getPaymentsByDateRange(LocalDate.now(ZoneId.systemDefault()), LocalDate.now(ZoneId.systemDefault())));
    }

    @Test
    void createPayment_shouldReturnCreatedPayment_whenValidPayment() {
        final var user = TestDataBuilder.createUser("testuser", "test@example.com");
        final var newPayment = new PaymentDto(user.getId(), 1L, 2L, new BigDecimal("100.00"),
                LocalDate.now(ZoneId.systemDefault()), "New Payment", null, null);
        final var createdPayment = MockDataUtil.createPaymentResponseDto();
        createdPayment.setId(1L);
        createdPayment.setComment("New Payment");
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdPayment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.createPayment(newPayment, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.paymentService.createPayment(newPayment);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getComment()).isEqualTo("New Payment");
        verify(this.paymentClient).createPayment(newPayment, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void createPayment_shouldThrowServiceException_whenCreationFails() {
        final var newPayment = TestDataBuilder.createUniquePayment();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.createPayment(newPayment, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(ResponseTestUtil.createBadRequestResponse());
        assertThatThrownBy(() -> this.paymentService.createPayment(newPayment)).isInstanceOf(ServiceException.class);
    }

    @Test
    void updatePayment_shouldReturnUpdatedPayment_whenValidPayment() {
        final var paymentToUpdate = TestDataBuilder.createUniquePayment();
        paymentToUpdate.setId(1L);
        paymentToUpdate.setComment("Updated Payment");
        final var updatedPayment = MockDataUtil.createPaymentResponseDto();
        updatedPayment.setId(1L);
        updatedPayment.setComment("Updated Payment");
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedPayment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.updatePayment(1L, paymentToUpdate, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.paymentService.updatePayment(paymentToUpdate);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getComment()).isEqualTo("Updated Payment");
        verify(this.paymentClient).updatePayment(1L, paymentToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getAllPayments_shouldNormalizeInvalidPaginationValues() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getAllPayments(0, 20, TestUtil.TEST_AUTH_HEADER)).thenReturn(Collections.emptyList());

        final var result = this.paymentService.getAllPayments(-3, 0);

        assertThat(result).isEmpty();
        verify(this.paymentClient).getAllPayments(0, 20, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getRecentPayments_shouldCapLimitAtHundred() {
        final var expectedPayments = Collections.<PaymentResponseDto>emptyList();
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getRecentPayments(100, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPayments);

        final var result = this.paymentService.getRecentPayments(1000);

        assertThat(result).isEmpty();
        verify(this.paymentClient).getRecentPayments(100, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deletePayment_shouldReturnTrue_whenDeletionSuccessful() {
        final var paymentId = 1L;
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.deletePayment(paymentId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final boolean result = this.paymentService.deletePayment(paymentId);

        assertThat(result).isTrue();
        verify(this.paymentClient).deletePayment(paymentId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deletePayment_shouldReturnFalse_whenDeletionFails() {
        final var paymentId = 1L;
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.deletePayment(paymentId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final boolean result = this.paymentService.deletePayment(paymentId);

        assertThat(result).isFalse();
    }

    @Test
    void getPaymentById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService.getPaymentById(1L));
    }

    @Test
    void createPayment_shouldThrowAuthenticationException_whenNotAuthenticated() {
        final var newPayment = TestDataBuilder.createUniquePayment();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService.createPayment(newPayment));
    }

    @Test
    void getAllPayments_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getAllPayments(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.paymentService.getAllPayments(0, 50));
    }

    @Test
    void getPaymentById_shouldThrowServiceException_whenWebApplicationExceptionOccurs() {
        final var paymentId = 1L;
        final WebApplicationException webException = ExceptionTestUtil.createWebApplicationException(500);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPayment(paymentId, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        assertThatThrownBy(() -> this.paymentService.getPaymentById(paymentId)).isInstanceOf(ServiceException.class)
                .hasMessage("Backend request failed");
    }

    @Test
    void getPaymentsByUser_shouldReturnEmptyList_whenUserHasNoPayments() {
        final var userId = 1L;
        final var emptyList = Collections.<PaymentResponseDto>emptyList();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsByUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(emptyList);

        final var result = this.paymentService.getPaymentsByUser(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void getPaymentsBySourceAccount_shouldReturnPayments_whenSourceAccountExists() {
        final var sourceId = 1L;
        final var payment = MockDataUtil.createPaymentResponseDto(1L, "testuser", sourceId, "Source Account", 2L,
                "Target Account", new BigDecimal("100.00"), "Source Payment");
        final var expected = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsBySourceAccount(sourceId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.paymentService.getPaymentsBySourceAccount(sourceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComment()).isEqualTo("Source Payment");
    }

    @Test
    void getPaymentsBySourceAccount_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService.getPaymentsBySourceAccount(1L));
    }

    @Test
    void getPaymentsBySourceAccount_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var sourceId = 1L;
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsBySourceAccount(sourceId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil
                .assertThrowsBackendConnectionFailed(() -> this.paymentService.getPaymentsBySourceAccount(sourceId));
    }

    @Test
    void getPaymentsBySourceAccount_shouldThrowAuthenticationException_when401Error() {
        final var sourceId = 1L;
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsBySourceAccount(sourceId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.paymentService.getPaymentsBySourceAccount(sourceId),
                this.authService);
    }

    @Test
    void getPaymentsByTargetAccount_shouldReturnPayments_whenTargetAccountExists() {
        final var targetId = 2L;
        final var payment = MockDataUtil.createPaymentResponseDto(1L, "testuser", 1L, "Source Account", targetId,
                "Target Account", new BigDecimal("100.00"), "Target Payment");
        final var expected = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsByTargetAccount(targetId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.paymentService.getPaymentsByTargetAccount(targetId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComment()).isEqualTo("Target Payment");
    }

    @Test
    void getPaymentsByTargetAccount_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.paymentService.getPaymentsByTargetAccount(2L));
    }

    @Test
    void getPaymentsByTargetAccount_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var targetId = 2L;
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsByTargetAccount(targetId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil
                .assertThrowsBackendConnectionFailed(() -> this.paymentService.getPaymentsByTargetAccount(targetId));
    }

    @Test
    void getPaymentsByTargetAccount_shouldThrowAuthenticationException_when401Error() {
        final var targetId = 2L;
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.getPaymentsByTargetAccount(targetId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.paymentService.getPaymentsByTargetAccount(targetId),
                this.authService);
    }

    @Test
    void searchPayments_shouldReturnMatchingPayments_whenQueryProvided() {
        final String query = "test";
        final var payment = MockDataUtil.createPaymentResponseDto(1L, "testuser", 1L, "Source Account", 2L,
                "Target Account", new BigDecimal("100.00"), "Search Payment");
        final var expected = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.searchPayments(query, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.paymentService.searchPayments(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComment()).isEqualTo("Search Payment");
    }

    @Test
    void searchPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final String query = "test";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.searchPayments(query, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.paymentService.searchPayments(query));
    }

    @Test
    void searchPayments_shouldThrowAuthenticationException_when401Error() {
        final String query = "test";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.paymentClient.searchPayments(query, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.paymentService.searchPayments(query), this.authService);
    }
}
