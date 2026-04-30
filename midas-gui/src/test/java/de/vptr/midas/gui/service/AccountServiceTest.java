package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.AccountClient;
import de.vptr.midas.gui.dto.UserAccountMetaDto;
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
class AccountServiceTest {

        @RestClient
        @Inject
        @Mock
        AccountClient accountClient;

        @Mock
        AuthService authService;

        @InjectMocks
        AccountService accountService;

        @Test
        void getAllAccounts_shouldReturnAccountList_whenAuthenticationProvided() {
                // Given
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var account1 = MockDataUtil.createAccountResponseDto(1L, "Account 1");
                final var account2 = MockDataUtil.createAccountResponseDto(2L, "Account 2");
                final var expectedAccounts = Arrays.asList(account1, account2);

                when(this.accountClient.getAllAccounts(0, 50, authHeader)).thenReturn(expectedAccounts);

                // When
                final var result = this.accountService.getAllAccounts(authHeader, 0, 50);

                // Then
                assertThat(result).hasSize(2);
                assertThat(result).containsExactly(account1, account2);
                verify(this.accountClient).getAllAccounts(0, 50, authHeader);
        }

        @Test
        void getAllAccounts_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                // When & Then
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.getAllAccounts(null, 0, 50));
        }

        @Test
        void getAllAccounts_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                // Given
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getAllAccounts(0, 50, authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                // When & Then
                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getAllAccounts(authHeader, 0, 50));
        }

        @Test
        void getAllAccounts_shouldThrowAuthenticationException_when401Error() {
                // Given
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getAllAccounts(0, 50, authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                // When & Then
                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getAllAccounts(authHeader, 0, 50),
                                this.authService);
        }

        @Test
        void getAccountById_shouldReturnAccount_whenAccountExists() {
                // Given
                final var accountId = 1L;
                final var expectedAccount = MockDataUtil.createAccountResponseDto(accountId, "Test Account");
                final var mockResponse = ResponseTestUtil.createOkResponse(expectedAccount);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

                // When
                final var result = this.accountService.getAccountById(accountId);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(accountId);
                assertThat(result.get().getName()).isEqualTo("Test Account");
        }

        @Test
        void getAccountById_shouldReturnEmpty_whenAccountNotFound() {
                // Given
                final var accountId = 999L;

                ServiceTestUtil.setupAuthHeader(this.authService);
                ServiceTestUtil.testReturnEmptyOn404(
                                () -> when(this.accountClient.getAccount(accountId, TestUtil.TEST_AUTH_HEADER))
                                                .thenReturn(ResponseTestUtil.createNotFoundResponse()),
                                () -> this.accountService.getAccountById(accountId));
        }

        @Test
        void getAccountByName_shouldReturnAccount_whenAccountExists() {
                // Given
                final String accountName = "Test Account";
                final var expectedAccount = MockDataUtil.createAccountResponseDto(1L, accountName);
                final var mockResponse = ResponseTestUtil.createOkResponse(expectedAccount);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccountByName(accountName, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                // When
                final var result = this.accountService.getAccountByName(accountName);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().getName()).isEqualTo(accountName);
        }

        @Test
        void getAccountsByUser_shouldReturnUserAccounts_whenUserExists() {
                // Given
                final var userId = 1L;
                final var account1 = MockDataUtil.createAccountResponseDto(1L, "Account 1");
                final var account2 = MockDataUtil.createAccountResponseDto(2L, "Account 2");
                final var expectedAccounts = Arrays.asList(account1, account2);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccountsByUser(userId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(expectedAccounts);

                // When
                final var result = this.accountService.getAccountsByUser(userId);

                // Then
                assertThat(result).hasSize(2);
                assertThat(result).containsExactly(account1, account2);
        }

        @Test
        void getAccountsByUser_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.getAccountsByUser(1L));
        }

        @Test
        void searchAccounts_shouldReturnMatchingAccounts_whenQueryProvided() {
                // Given
                final String query = "test";
                final var account = MockDataUtil.createAccountResponseDto(1L, "Test Account");
                final var expectedAccounts = Collections.singletonList(account);

                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.searchAccounts(query, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedAccounts);

                // When
                final var result = this.accountService.searchAccounts(query, authHeader);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getName()).isEqualTo("Test Account");
        }

        @Test
        void searchAccounts_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.searchAccounts("test", null));
        }

        @Test
        void createAccount_shouldReturnCreatedAccount_whenValidAccount() {
                // Given
                final var newAccount = MockDataUtil.createAccountDto();
                newAccount.setName("New Account");
                final var createdAccountResponse = TestDataBuilder.createAccountResponse(1L,
                                "New Account");
                final var mockResponse = ResponseTestUtil.createCreatedResponse(createdAccountResponse);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.createAccount(newAccount, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

                // When
                final var result = this.accountService.createAccount(newAccount);

                // Then
                assertThat(result.getId()).isEqualTo(1L);
                assertThat(result.getName()).isEqualTo("New Account");
                verify(this.accountClient).createAccount(newAccount, TestUtil.TEST_AUTH_HEADER);
        }

        @Test
        void createAccount_shouldThrowServiceException_whenCreationFails() {
                // Given
                final var newAccount = MockDataUtil.createAccountDto();

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.createAccount(newAccount, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(ResponseTestUtil.createBadRequestResponse());
                assertThatThrownBy(() -> this.accountService.createAccount(newAccount))
                                .isInstanceOf(ServiceException.class);
        }

        @Test
        void createAccount_shouldExtractBackendErrorMessageAndCloseResponse() {
                final var newAccount = MockDataUtil.createAccountDto();
                final var response = Mockito.mock(jakarta.ws.rs.core.Response.class);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.createAccount(newAccount, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);
                when(response.getStatus()).thenReturn(400);
                when(response.hasEntity()).thenReturn(true);
                when(response.readEntity(String.class)).thenReturn("{\"message\":\"Account name already exists\"}");

                assertThatThrownBy(() -> this.accountService.createAccount(newAccount))
                                .isInstanceOf(ServiceException.class)
                                .hasMessage("Failed to create account: Account name already exists");

                verify(response).close();
        }

        @Test
        void updateAccount_shouldReturnUpdatedAccount_whenValidAccount() {
                // Given
                final var accountToUpdate = MockDataUtil.createAccountDto(1L, "Updated Account");
                final var updatedAccountResponse = TestDataBuilder.createAccountResponse(1L,
                                "Updated Account");
                final var mockResponse = ResponseTestUtil.createOkResponse(updatedAccountResponse);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.updateAccount(1L, accountToUpdate, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                // When
                final var result = this.accountService.updateAccount(accountToUpdate);

                // Then
                assertThat(result.getId()).isEqualTo(1L);
                assertThat(result.getName()).isEqualTo("Updated Account");
                verify(this.accountClient).updateAccount(1L, accountToUpdate, TestUtil.TEST_AUTH_HEADER);
        }

        @Test
        void deleteAccount_shouldReturnTrue_whenDeletionSuccessful() {
                // Given
                final var accountId = 1L;
                final var mockResponse = ResponseTestUtil.createMockResponse(204);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.deleteAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

                // When
                final boolean result = this.accountService.deleteAccount(accountId);

                // Then
                assertThat(result).isTrue();
                verify(this.accountClient).deleteAccount(accountId, TestUtil.TEST_AUTH_HEADER);
        }

        @Test
        void deleteAccount_shouldReturnFalse_whenDeletionFails() {
                // Given
                final var accountId = 1L;
                final var mockResponse = ResponseTestUtil.createBadRequestResponse();

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.deleteAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

                // When
                final boolean result = this.accountService.deleteAccount(accountId);

                // Then
                assertThat(result).isFalse();
        }

        @Test
        void getAccountById_shouldThrowAuthenticationException_whenNotAuthenticated() {
                // When & Then
                ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getAccountById(1L));
        }

        @Test
        void createAccount_shouldThrowAuthenticationException_whenNotAuthenticated() {
                // Given
                final var newAccount = MockDataUtil.createAccountDto();
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                // When & Then
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.createAccount(newAccount));
        }

        @Test
        void getAllAccounts_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
                // Given
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getAllAccounts(0, 50, authHeader))
                                .thenThrow(new RuntimeException("Unexpected error"));

                // When & Then
                ExceptionTestUtil.assertThrowsUnexpectedError(
                                () -> this.accountService.getAllAccounts(authHeader, 0, 50));
        }

        @Test
        void getAccountById_shouldThrowServiceException_whenWebApplicationExceptionOccurs() {
                // Given
                final var accountId = 1L;
                final WebApplicationException webException = ExceptionTestUtil.createWebApplicationException(500);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

                // When & Then
                assertThatThrownBy(() -> this.accountService.getAccountById(accountId))
                                .isInstanceOf(ServiceException.class)
                                .hasMessage("Backend error: 500");
        }

        // ===================== getMyAccounts tests =====================

        @Test
        void getMyAccounts_shouldReturnAccountList_whenAuthenticationProvided() {
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var account = MockDataUtil.createAccountResponseDto(1L, "My Account");
                final var expected = Collections.singletonList(account);
                when(this.accountClient.getMyAccounts(authHeader)).thenReturn(expected);

                final var result = this.accountService.getMyAccounts(authHeader);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getName()).isEqualTo("My Account");
        }

        @Test
        void getMyAccounts_shouldThrowServiceException_whenAuthHeaderIsNull() {
                ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getMyAccounts(null));
        }

        @Test
        void getMyAccounts_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccounts(authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getMyAccounts(authHeader));
        }

        @Test
        void getMyAccounts_shouldThrowAuthenticationException_when401Error() {
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccounts(authHeader)).thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getMyAccounts(authHeader), this.authService);
        }

        // ===================== getMyAccountsWithBalances tests =====================

        @Test
        void getMyAccountsWithBalances_shouldReturnAccountList_whenAuthenticationProvided() {
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var account = MockDataUtil.createAccountResponseDto(1L, "My Account");
                final var expected = Collections.singletonList(account);
                when(this.accountClient.getMyAccountsWithBalances(authHeader)).thenReturn(expected);

                final var result = this.accountService.getMyAccountsWithBalances(authHeader);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getName()).isEqualTo("My Account");
        }

        @Test
        void getMyAccountsWithBalances_shouldThrowServiceException_whenAuthHeaderIsNull() {
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.getMyAccountsWithBalances(null));
        }

        @Test
        void getMyAccountsWithBalances_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountsWithBalances(authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getMyAccountsWithBalances(authHeader));
        }

        @Test
        void getMyAccountsWithBalances_shouldThrowAuthenticationException_when401Error() {
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountsWithBalances(authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getMyAccountsWithBalances(authHeader), this.authService);
        }

        // ===================== getOutgoingPayments tests =====================

        @Test
        void getOutgoingPayments_shouldReturnPaymentList_whenAccountExists() {
                final var accountId = 1L;
                final var payment = TestDataBuilder.createUniquePayment();
                final var mockResponse = ResponseTestUtil.createOkResponse(Collections.singletonList(payment));

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                final var result = this.accountService.getOutgoingPayments(accountId);

                assertThat(result).containsExactly(payment);
        }

        @Test
        void getOutgoingPayments_shouldReturnEmptyList_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                final var result = this.accountService.getOutgoingPayments(1L);

                assertThat(result).isEmpty();
        }

        @Test
        void getOutgoingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getOutgoingPayments(accountId));
        }

        @Test
        void getOutgoingPayments_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getOutgoingPayments(accountId), this.authService);
        }

        // ===================== getIncomingPayments tests =====================

        @Test
        void getIncomingPayments_shouldReturnPaymentList_whenAccountExists() {
                final var accountId = 1L;
                final var payment = TestDataBuilder.createUniquePayment();
                final var mockResponse = ResponseTestUtil.createOkResponse(Collections.singletonList(payment));

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                final var result = this.accountService.getIncomingPayments(accountId);

                assertThat(result).containsExactly(payment);
        }

        @Test
        void getIncomingPayments_shouldReturnEmptyList_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                final var result = this.accountService.getIncomingPayments(1L);

                assertThat(result).isEmpty();
        }

        @Test
        void getIncomingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getIncomingPayments(accountId));
        }

        @Test
        void getIncomingPayments_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getIncomingPayments(accountId), this.authService);
        }

        // ===================== getAssociatedUsers tests =====================

        @Test
        void getAssociatedUsers_shouldReturnUserList_whenAccountExists() {
                final var accountId = 1L;
                final var user = MockDataUtil.createUserResponseDto(1L, "testuser", "test@example.com");
                final var expected = Collections.singletonList(user);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAssociatedUsers(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

                final var result = this.accountService.getAssociatedUsers(accountId);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        void getAssociatedUsers_shouldReturnEmptyList_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                final var result = this.accountService.getAssociatedUsers(1L);

                assertThat(result).isEmpty();
        }

        @Test
        void getAssociatedUsers_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAssociatedUsers(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getAssociatedUsers(accountId));
        }

        @Test
        void getAssociatedUsers_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAssociatedUsers(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getAssociatedUsers(accountId), this.authService);
        }

        // ===================== associateUserWithAccount tests =====================

        @Test
        void associateUserWithAccount_shouldReturnTrue_whenAssociationSuccessful() {
                final var accountId = 1L;
                final var userId = 2L;
                final var mockResponse = ResponseTestUtil.createMockResponse(201);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                final boolean result = this.accountService.associateUserWithAccount(accountId, userId);

                assertThat(result).isTrue();
        }

        @Test
        void associateUserWithAccount_shouldReturnFalse_whenNon204Response() {
                final var accountId = 1L;
                final var userId = 2L;
                final var mockResponse = ResponseTestUtil.createBadRequestResponse();

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                final boolean result = this.accountService.associateUserWithAccount(accountId, userId);

                assertThat(result).isFalse();
        }

        @Test
        void associateUserWithAccount_shouldThrowServiceException_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.associateUserWithAccount(1L, 2L));
        }

        @Test
        void associateUserWithAccount_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var userId = 2L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.associateUserWithAccount(accountId, userId));
        }

        @Test
        void associateUserWithAccount_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var userId = 2L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.associateUserWithAccount(accountId, userId),
                                this.authService);
        }

        @Test
        void associateUserWithAccount_shouldThrowServiceException_when409Conflict() {
                final var accountId = 1L;
                final var userId = 2L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.createWebApplicationException(409));

                ExceptionTestUtil.assertThrowsServiceException(
                                () -> this.accountService.associateUserWithAccount(accountId, userId),
                                "User is already associated with this account");
        }

        // ===================== removeUserFromAccount tests =====================

        @Test
        void removeUserFromAccount_shouldReturnTrue_whenRemovalSuccessful() {
                final var accountId = 1L;
                final var userId = 2L;
                final var mockResponse = ResponseTestUtil.createMockResponse(204);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                final boolean result = this.accountService.removeUserFromAccount(accountId, userId);

                assertThat(result).isTrue();
        }

        @Test
        void removeUserFromAccount_shouldReturnFalse_whenNon204Response() {
                final var accountId = 1L;
                final var userId = 2L;
                final var mockResponse = ResponseTestUtil.createBadRequestResponse();

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                final boolean result = this.accountService.removeUserFromAccount(accountId, userId);

                assertThat(result).isFalse();
        }

        @Test
        void removeUserFromAccount_shouldThrowServiceException_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.removeUserFromAccount(1L, 2L));
        }

        @Test
        void removeUserFromAccount_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var userId = 2L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.removeUserFromAccount(accountId, userId));
        }

        @Test
        void removeUserFromAccount_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var userId = 2L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.removeUserFromAccount(accountId, userId), this.authService);
        }

        // ===================== getAccountBalance tests =====================

        @Test
        void getAccountBalance_shouldReturnBalance_whenAccountExists() {
                final var accountId = 1L;
                final var mockResponse = ResponseTestUtil.createOkResponse("{\"balance\": 123.45}");

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                final var result = this.accountService.getAccountBalance(accountId);

                assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
        }

        @Test
        void getAccountBalance_shouldThrowServiceException_whenPayloadIsMalformed() {
                final var accountId = 1L;
                final var mockResponse = ResponseTestUtil.createOkResponse("{\"balance\": \"invalid\"}");

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenReturn(mockResponse);

                ExceptionTestUtil.assertThrowsServiceException(
                                () -> this.accountService.getAccountBalance(accountId),
                                "Malformed balance payload");
        }

        @Test
        void getAccountBalance_shouldCloseResponseAfterParsingPayload() {
                final var accountId = 1L;
                final var response = Mockito.mock(jakarta.ws.rs.core.Response.class);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);
                when(response.getStatus()).thenReturn(200);
                when(response.readEntity(String.class)).thenReturn("{\"balance\": 123.45}");

                final var result = this.accountService.getAccountBalance(accountId);

                assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
                verify(response).close();
        }

        @Test
        void getAccountBalance_shouldThrowServiceException_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getAccountBalance(1L));
        }

        @Test
        void getAccountBalance_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getAccountBalance(accountId));
        }

        @Test
        void getAccountBalance_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getAccountBalance(accountId), this.authService);
        }

        // ===================== getMyAccountBalance tests =====================

        @Test
        void getMyAccountBalance_shouldReturnBalance_whenAccountExists() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var mockResponse = ResponseTestUtil.createOkResponse("{\"balance\": 456.78}");

                when(this.accountClient.getMyAccountBalance(accountId, authHeader)).thenReturn(mockResponse);

                final var result = this.accountService.getMyAccountBalance(accountId, authHeader);

                assertThat(result).isEqualByComparingTo(new BigDecimal("456.78"));
        }

        @Test
        void getMyAccountBalance_shouldThrowServiceException_whenPayloadHasNoBalance() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var mockResponse = ResponseTestUtil.createOkResponse("{\"otherField\": 1}");

                when(this.accountClient.getMyAccountBalance(accountId, authHeader)).thenReturn(mockResponse);

                ExceptionTestUtil.assertThrowsServiceException(
                                () -> this.accountService.getMyAccountBalance(accountId, authHeader),
                                "Malformed balance payload: missing balance");
        }

        @Test
        void getMyAccountBalance_shouldThrowServiceException_whenAuthHeaderIsNull() {
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.getMyAccountBalance(1L, null));
        }

        @Test
        void getMyAccountBalance_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountBalance(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getMyAccountBalance(accountId, authHeader));
        }

        @Test
        void getMyAccountBalance_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountBalance(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getMyAccountBalance(accountId, authHeader), this.authService);
        }

        // ===================== getMyAccountPayments tests =====================

        @Test
        void getMyAccountPayments_shouldReturnPayments_whenAccountExists() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = MockDataUtil.createPaymentResponseDto();
                final var expected = Collections.singletonList(payment);
                when(this.accountClient.getMyAccountPayments(accountId, authHeader)).thenReturn(expected);

                final var result = this.accountService.getMyAccountPayments(accountId, authHeader);

                assertThat(result).hasSize(1);
        }

        @Test
        void getMyAccountPayments_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.getMyAccountPayments(1L, null));
        }

        @Test
        void getMyAccountPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountPayments(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getMyAccountPayments(accountId, authHeader));
        }

        @Test
        void getMyAccountPayments_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountPayments(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getMyAccountPayments(accountId, authHeader),
                                this.authService);
        }

        // ===================== getMyAccountOutgoingPayments tests
        // =====================

        @Test
        void getMyAccountOutgoingPayments_shouldReturnPayments_whenAccountExists() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = MockDataUtil.createPaymentResponseDto();
                final var expected = Collections.singletonList(payment);
                when(this.accountClient.getMyAccountOutgoingPayments(accountId, authHeader)).thenReturn(expected);

                final var result = this.accountService.getMyAccountOutgoingPayments(accountId, authHeader);

                assertThat(result).hasSize(1);
        }

        @Test
        void getMyAccountOutgoingPayments_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.getMyAccountOutgoingPayments(1L, null));
        }

        @Test
        void getMyAccountOutgoingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountOutgoingPayments(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getMyAccountOutgoingPayments(accountId, authHeader));
        }

        @Test
        void getMyAccountOutgoingPayments_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountOutgoingPayments(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getMyAccountOutgoingPayments(accountId, authHeader),
                                this.authService);
        }

        // ===================== getMyAccountIncomingPayments tests
        // =====================

        @Test
        void getMyAccountIncomingPayments_shouldReturnPayments_whenAccountExists() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = MockDataUtil.createPaymentResponseDto();
                final var expected = Collections.singletonList(payment);
                when(this.accountClient.getMyAccountIncomingPayments(accountId, authHeader)).thenReturn(expected);

                final var result = this.accountService.getMyAccountIncomingPayments(accountId, authHeader);

                assertThat(result).hasSize(1);
        }

        @Test
        void getMyAccountIncomingPayments_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.getMyAccountIncomingPayments(1L, null));
        }

        @Test
        void getMyAccountIncomingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountIncomingPayments(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.getMyAccountIncomingPayments(accountId, authHeader));
        }

        @Test
        void getMyAccountIncomingPayments_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                when(this.accountClient.getMyAccountIncomingPayments(accountId, authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.getMyAccountIncomingPayments(accountId, authHeader),
                                this.authService);
        }

        // ===================== createMyAccountPayment tests =====================

        @Test
        void createMyAccountPayment_shouldReturnPayment_whenValid() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = TestDataBuilder.createUniquePayment();
                final var createdPayment = MockDataUtil.createPaymentResponseDto();
                createdPayment.setId(1L);
                final var mockResponse = ResponseTestUtil.createCreatedResponse(createdPayment);
                when(this.accountClient.createMyAccountPayment(accountId, payment, authHeader))
                                .thenReturn(mockResponse);

                final var result = this.accountService.createMyAccountPayment(accountId, payment, authHeader);

                assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        void createMyAccountPayment_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                final var payment = TestDataBuilder.createUniquePayment();
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.createMyAccountPayment(1L, payment, null));
        }

        @Test
        void createMyAccountPayment_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = TestDataBuilder.createUniquePayment();
                when(this.accountClient.createMyAccountPayment(accountId, payment, authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.createMyAccountPayment(accountId, payment, authHeader));
        }

        @Test
        void createMyAccountPayment_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = TestDataBuilder.createUniquePayment();
                when(this.accountClient.createMyAccountPayment(accountId, payment, authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.createMyAccountPayment(accountId, payment, authHeader),
                                this.authService);
        }

        // ===================== updateMyAccountPayment tests =====================

        @Test
        void updateMyAccountPayment_shouldReturnPayment_whenValid() {
                final var accountId = 1L;
                final var paymentId = 2L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = TestDataBuilder.createUniquePayment();
                payment.setId(paymentId);
                final var updatedPayment = MockDataUtil.createPaymentResponseDto();
                updatedPayment.setId(paymentId);
                final var mockResponse = ResponseTestUtil.createOkResponse(updatedPayment);

                when(this.accountClient.updateMyAccountPayment(accountId, paymentId, payment, authHeader))
                                .thenReturn(mockResponse);

                final var result = this.accountService.updateMyAccountPayment(accountId, paymentId, payment,
                                authHeader);

                assertThat(result.getId()).isEqualTo(paymentId);
        }

        @Test
        void updateMyAccountPayment_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                final var payment = TestDataBuilder.createUniquePayment();
                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.updateMyAccountPayment(1L, 2L, payment, null));
        }

        @Test
        void updateMyAccountPayment_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var paymentId = 2L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = TestDataBuilder.createUniquePayment();
                when(this.accountClient.updateMyAccountPayment(accountId, paymentId, payment, authHeader))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.updateMyAccountPayment(accountId, paymentId, payment,
                                                authHeader));
        }

        @Test
        void updateMyAccountPayment_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var paymentId = 2L;
                final var authHeader = TestUtil.TEST_AUTH_HEADER;
                final var payment = TestDataBuilder.createUniquePayment();
                when(this.accountClient.updateMyAccountPayment(accountId, paymentId, payment, authHeader))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.updateMyAccountPayment(accountId, paymentId, payment,
                                                authHeader),
                                this.authService);
        }

        // ===================== updateUserAccountMeta tests =====================

        @Test
        void updateUserAccountMeta_shouldSucceed_when204Returned() {
                final var accountId = 1L;
                final var userId = 2L;
                final var mockResponse = ResponseTestUtil.createMockResponse(204);

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.updateUserAccountMeta(
                                org.mockito.ArgumentMatchers.eq(accountId),
                                org.mockito.ArgumentMatchers.eq(userId),
                                org.mockito.ArgumentMatchers.any(UserAccountMetaDto.class),
                                org.mockito.ArgumentMatchers.eq(TestUtil.TEST_AUTH_HEADER)))
                                .thenReturn(mockResponse);

                // Should not throw
                this.accountService.updateUserAccountMeta(accountId, userId, true);
        }

        @Test
        void updateUserAccountMeta_shouldThrowServiceException_whenNon204Returned() {
                final var accountId = 1L;
                final var userId = 2L;
                final var mockResponse = ResponseTestUtil.createBadRequestResponse();

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.updateUserAccountMeta(
                                org.mockito.ArgumentMatchers.eq(accountId),
                                org.mockito.ArgumentMatchers.eq(userId),
                                org.mockito.ArgumentMatchers.any(UserAccountMetaDto.class),
                                org.mockito.ArgumentMatchers.eq(TestUtil.TEST_AUTH_HEADER)))
                                .thenReturn(mockResponse);

                ExceptionTestUtil.assertThrowsServiceException(
                                () -> this.accountService.updateUserAccountMeta(accountId, userId, false),
                                "Failed to update user-account meta: HTTP 400");
        }

        @Test
        void updateUserAccountMeta_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
                when(this.authService.getAuthorizationHeader()).thenReturn(null);

                ExceptionTestUtil.assertThrowsAuthenticationRequired(
                                () -> this.accountService.updateUserAccountMeta(1L, 2L, true));
        }

        @Test
        void updateUserAccountMeta_shouldThrowServiceException_whenProcessingExceptionOccurs() {
                final var accountId = 1L;
                final var userId = 2L;

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.updateUserAccountMeta(
                                org.mockito.ArgumentMatchers.eq(accountId),
                                org.mockito.ArgumentMatchers.eq(userId),
                                org.mockito.ArgumentMatchers.any(UserAccountMetaDto.class),
                                org.mockito.ArgumentMatchers.eq(TestUtil.TEST_AUTH_HEADER)))
                                .thenThrow(ExceptionTestUtil.createProcessingException());

                ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                                () -> this.accountService.updateUserAccountMeta(accountId, userId, true));
        }

        @Test
        void updateUserAccountMeta_shouldThrowAuthenticationException_when401Error() {
                final var accountId = 1L;
                final var userId = 2L;

                ServiceTestUtil.setupAuthHeader(this.authService);
                when(this.accountClient.updateUserAccountMeta(
                                org.mockito.ArgumentMatchers.eq(accountId),
                                org.mockito.ArgumentMatchers.eq(userId),
                                org.mockito.ArgumentMatchers.any(UserAccountMetaDto.class),
                                org.mockito.ArgumentMatchers.eq(TestUtil.TEST_AUTH_HEADER)))
                                .thenThrow(ExceptionTestUtil.create401Exception());

                ExceptionTestUtil.assertHandles401WithLogout(
                                () -> this.accountService.updateUserAccountMeta(accountId, userId, true),
                                this.authService);
        }
}
