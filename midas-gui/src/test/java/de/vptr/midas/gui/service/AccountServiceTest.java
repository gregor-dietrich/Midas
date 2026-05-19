package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestDataBuilder;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "PMD.LinguisticNaming", "PMD.UnitTestShouldIncludeAssert", "PMD.TooManyMethods" })
class AccountServiceTest {

    @Mock
    @RestClient
    AccountClient accountClient;

    @Mock
    AuthService authService;

    @InjectMocks
    AccountService accountService;

    @Test
    void getAllAccounts_shouldReturnAccountList_whenAuthenticationProvided() {
        final var account1 = MockDataUtil.createAccountResponseDto(1L, "Account 1");
        final var account2 = MockDataUtil.createAccountResponseDto(2L, "Account 2");
        final var expectedAccounts = Arrays.asList(account1, account2);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAllAccounts(0, 50, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedAccounts);

        final var result = this.accountService.getAllAccounts(0, 50);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(account1, account2);
        verify(this.accountClient).getAllAccounts(0, 50, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getAllAccounts_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getAllAccounts(0, 50));
    }

    @Test
    void getAllAccounts_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAllAccounts(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.accountService.getAllAccounts(0, 50));
    }

    @Test
    void getAllAccounts_shouldThrowAuthenticationException_when401Error() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAllAccounts(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getAllAccounts(0, 50), this.authService);
    }

    @Test
    void getAccountById_shouldReturnAccount_whenAccountExists() {
        final var accountId = "1";
        final var expectedAccount = MockDataUtil.createAccountResponseDto(1L, "Test Account");
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedAccount);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.accountService.getAccountById(accountId);

        assertThat(result).isPresent();
        assertThat(result.get().getPublicId()).isEqualTo(accountId);
        assertThat(result.get().getName()).isEqualTo("Test Account");
    }

    @Test
    void getAccountById_shouldReturnEmpty_whenAccountNotFound() {
        final var accountId = "999";

        ServiceTestUtil.setupAuthHeader(this.authService);
        ServiceTestUtil.testReturnEmptyOn404(
                () -> when(this.accountClient.getAccount(accountId, TestUtil.TEST_AUTH_HEADER))
                        .thenReturn(ResponseTestUtil.createNotFoundResponse()),
                () -> this.accountService.getAccountById(accountId));
    }

    @Test
    void getAccountByName_shouldReturnAccount_whenAccountExists() {
        final String accountName = "Test Account";
        final var expectedAccount = MockDataUtil.createAccountResponseDto(1L, accountName);
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedAccount);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountByName(accountName, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.accountService.getAccountByName(accountName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(accountName);
    }

    @Test
    void getAccountByName_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.getAccountByName("Test Account"));
    }

    @Test
    void getAccountsByUser_shouldReturnUserAccounts_whenUserExists() {
        final var userId = "1";
        final var account1 = MockDataUtil.createAccountResponseDto(1L, "Account 1");
        final var account2 = MockDataUtil.createAccountResponseDto(2L, "Account 2");
        final var expectedAccounts = Arrays.asList(account1, account2);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountsByUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedAccounts);

        final var result = this.accountService.getAccountsByUser(userId);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(account1, account2);
    }

    @Test
    void getAccountsByUser_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getAccountsByUser("1"));
    }

    @Test
    void searchAccounts_shouldReturnMatchingAccounts_whenQueryProvided() {
        final String query = "test";
        final var account = MockDataUtil.createAccountResponseDto(1L, "Test Account");
        final var expectedAccounts = Collections.singletonList(account);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.searchAccounts(query, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedAccounts);

        final var result = this.accountService.searchAccounts(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Account");
    }

    @Test
    void searchAccounts_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.searchAccounts("test"));
    }

    @Test
    void createAccount_shouldReturnCreatedAccount_whenValidAccount() {
        final var newAccount = MockDataUtil.createAccountDto();
        newAccount.setName("New Account");
        final var createdAccountResponse = TestDataBuilder.createAccountResponse("1", "New Account");
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdAccountResponse);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.createAccount(newAccount, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.accountService.createAccount(newAccount);

        assertThat(result.getPublicId()).isEqualTo("1");
        assertThat(result.getName()).isEqualTo("New Account");
        verify(this.accountClient).createAccount(newAccount, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void createAccount_shouldThrowServiceException_whenCreationFails() {
        final var newAccount = MockDataUtil.createAccountDto();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.createAccount(newAccount, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(ResponseTestUtil.createBadRequestResponse());
        assertThatThrownBy(() -> this.accountService.createAccount(newAccount)).isInstanceOf(ServiceException.class);
    }

    @Test
    void createAccount_shouldExtractBackendErrorMessageAndCloseResponse() {
        final var newAccount = MockDataUtil.createAccountDto();
        final var response = Mockito.mock(Response.class);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.createAccount(newAccount, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);
        when(response.getStatus()).thenReturn(400);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(String.class)).thenReturn("{\"message\":\"Account name already exists\"}");

        assertThatThrownBy(() -> this.accountService.createAccount(newAccount)).isInstanceOf(ServiceException.class)
                .hasMessage("Failed to create account: Account name already exists");

        verify(response).close();
    }

    @Test
    void updateAccount_shouldReturnUpdatedAccount_whenValidAccount() {
        final var accountToUpdate = MockDataUtil.createAccountDto(1L, "Updated Account");
        final var updatedAccountResponse = TestDataBuilder.createAccountResponse("1", "Updated Account");
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedAccountResponse);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateAccount("1", accountToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        final var result = this.accountService.updateAccount(accountToUpdate);

        assertThat(result.getPublicId()).isEqualTo("1");
        assertThat(result.getName()).isEqualTo("Updated Account");
        verify(this.accountClient).updateAccount("1", accountToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteAccount_shouldReturnTrue_whenDeletionSuccessful() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.deleteAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final boolean result = this.accountService.deleteAccount(accountId);

        assertThat(result).isTrue();
        verify(this.accountClient).deleteAccount(accountId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteAccount_shouldReturnFalse_whenDeletionFails() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.deleteAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final boolean result = this.accountService.deleteAccount(accountId);

        assertThat(result).isFalse();
    }

    @Test
    void getAccountById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getAccountById("1"));
    }

    @Test
    void createAccount_shouldThrowAuthenticationException_whenNotAuthenticated() {
        final var newAccount = MockDataUtil.createAccountDto();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.createAccount(newAccount));
    }

    @Test
    void getAllAccounts_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAllAccounts(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.accountService.getAllAccounts(0, 50));
    }

    @Test
    void getAccountById_shouldThrowServiceException_whenWebApplicationExceptionOccurs() {
        final var accountId = "1";
        final WebApplicationException webException = ExceptionTestUtil.createWebApplicationException(500);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccount(accountId, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        assertThatThrownBy(() -> this.accountService.getAccountById(accountId)).isInstanceOf(ServiceException.class)
                .hasMessage("Backend request failed");
    }

    @Test
    void getMyAccounts_shouldReturnAccountList_whenAuthenticationProvided() {
        final var account = MockDataUtil.createAccountResponseDto(1L, "My Account");
        final var expected = Collections.singletonList(account);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccounts(TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.accountService.getMyAccounts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Account");
    }

    @Test
    void getMyAccounts_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getMyAccounts());
    }

    @Test
    void getMyAccounts_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccounts(TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.accountService.getMyAccounts());
    }

    @Test
    void getMyAccounts_shouldThrowAuthenticationException_when401Error() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccounts(TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getMyAccounts(), this.authService);
    }

    @Test
    void getMyAccountsWithBalances_shouldReturnAccountList_whenAuthenticationProvided() {
        final var account = MockDataUtil.createAccountResponseDto(1L, "My Account");
        final var expected = Collections.singletonList(account);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountsWithBalances(any(), any(), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(expected);

        final var result = this.accountService.getMyAccountsWithBalances(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Account");
    }

    @Test
    void getMyAccountsWithBalances_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.getMyAccountsWithBalances(null, null));
    }

    @Test
    void getMyAccountsWithBalances_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountsWithBalances(any(), any(), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil
                .assertThrowsBackendConnectionFailed(() -> this.accountService.getMyAccountsWithBalances(null, null));
    }

    @Test
    void getMyAccountsWithBalances_shouldThrowAuthenticationException_when401Error() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountsWithBalances(any(), any(), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getMyAccountsWithBalances(null, null),
                this.authService);
    }

    @Test
    void getOutgoingPayments_shouldReturnPaymentList_whenAccountExists() {
        final var accountId = "1";
        final var payment = TestDataBuilder.createUniquePayment();
        final var mockResponse = ResponseTestUtil.createOkResponse(Collections.singletonList(payment));

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.accountService.getOutgoingPayments(accountId);

        assertThat(result).containsExactly(payment);
    }

    @Test
    void getOutgoingPayments_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getOutgoingPayments("1"));
    }

    @Test
    void getOutgoingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.accountService.getOutgoingPayments(accountId));
    }

    @Test
    void getOutgoingPayments_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getOutgoingPayments(accountId),
                this.authService);
    }

    @Test
    void getIncomingPayments_shouldReturnPaymentList_whenAccountExists() {
        final var accountId = "1";
        final var payment = TestDataBuilder.createUniquePayment();
        final var mockResponse = ResponseTestUtil.createOkResponse(Collections.singletonList(payment));

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.accountService.getIncomingPayments(accountId);

        assertThat(result).containsExactly(payment);
    }

    @Test
    void getIncomingPayments_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getIncomingPayments("1"));
    }

    @Test
    void getIncomingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.accountService.getIncomingPayments(accountId));
    }

    @Test
    void getIncomingPayments_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getIncomingPayments(accountId),
                this.authService);
    }

    @Test
    void getAssociatedUsers_shouldReturnUserList_whenAccountExists() {
        final var accountId = "1";
        final var user = MockDataUtil.createUserResponseDto("1", "testuser", "test@example.com");
        final var expected = Collections.singletonList(user);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAssociatedUsers(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.accountService.getAssociatedUsers(accountId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void getAssociatedUsers_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getAssociatedUsers("1"));
    }

    @Test
    void getAssociatedUsers_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAssociatedUsers(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.accountService.getAssociatedUsers(accountId));
    }

    @Test
    void getAssociatedUsers_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAssociatedUsers(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getAssociatedUsers(accountId),
                this.authService);
    }

    @Test
    void associateUserWithAccount_shouldReturnTrue_whenAssociationSuccessful() {
        final var accountId = "1";
        final var userId = "2";
        final var mockResponse = ResponseTestUtil.createMockResponse(201);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        final boolean result = this.accountService.associateUserWithAccount(accountId, userId);

        assertThat(result).isTrue();
    }

    @Test
    void associateUserWithAccount_shouldReturnFalse_whenNon204Response() {
        final var accountId = "1";
        final var userId = "2";
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        final boolean result = this.accountService.associateUserWithAccount(accountId, userId);

        assertThat(result).isFalse();
    }

    @Test
    void associateUserWithAccount_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.associateUserWithAccount("1", "2"));
    }

    @Test
    void associateUserWithAccount_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        final var userId = "2";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.accountService.associateUserWithAccount(accountId, userId));
    }

    @Test
    void associateUserWithAccount_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        final var userId = "2";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(
                () -> this.accountService.associateUserWithAccount(accountId, userId), this.authService);
    }

    @Test
    void associateUserWithAccount_shouldThrowServiceException_when409Conflict() {
        final var accountId = "1";
        final var userId = "2";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.associateUserWithAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createWebApplicationException(409));

        ExceptionTestUtil.assertThrowsServiceException(
                () -> this.accountService.associateUserWithAccount(accountId, userId),
                "User is already associated with this account");
    }

    @Test
    void removeUserFromAccount_shouldReturnTrue_whenRemovalSuccessful() {
        final var accountId = "1";
        final var userId = "2";
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        final boolean result = this.accountService.removeUserFromAccount(accountId, userId);

        assertThat(result).isTrue();
    }

    @Test
    void removeUserFromAccount_shouldReturnFalse_whenNon204Response() {
        final var accountId = "1";
        final var userId = "2";
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        final boolean result = this.accountService.removeUserFromAccount(accountId, userId);

        assertThat(result).isFalse();
    }

    @Test
    void removeUserFromAccount_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.removeUserFromAccount("1", "2"));
    }

    @Test
    void removeUserFromAccount_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        final var userId = "2";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.accountService.removeUserFromAccount(accountId, userId));
    }

    @Test
    void removeUserFromAccount_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        final var userId = "2";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.removeUserFromAccount(accountId, userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.removeUserFromAccount(accountId, userId),
                this.authService);
    }

    @Test
    void getAccountBalance_shouldReturnBalance_whenAccountExists() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createOkResponse("{\"balance\": 123.45}");

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        final var result = this.accountService.getAccountBalance(accountId);

        assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    void getAccountBalance_shouldThrowServiceException_whenPayloadIsMalformed() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createOkResponse("{\"balance\": \"invalid\"}");

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        ExceptionTestUtil.assertThrowsServiceException(() -> this.accountService.getAccountBalance(accountId),
                "Malformed balance payload");
    }

    @Test
    void getAccountBalance_shouldCloseResponseAfterParsingPayload() {
        final var accountId = "1";
        final var response = Mockito.mock(Response.class);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(String.class)).thenReturn("{\"balance\": 123.45}");

        final var result = this.accountService.getAccountBalance(accountId);

        assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
        verify(response).close();
    }

    @Test
    void getAccountBalance_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.accountService.getAccountBalance("1"));
    }

    @Test
    void getAccountBalance_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.accountService.getAccountBalance(accountId));
    }

    @Test
    void getAccountBalance_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getAccountBalance(accountId),
                this.authService);
    }

    @Test
    void getMyAccountBalance_shouldReturnBalance_whenAccountExists() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createOkResponse("{\"balance\": 456.78}");

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountBalance(eq(accountId), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(mockResponse);

        final var result = this.accountService.getMyAccountBalance(accountId, null, null);

        assertThat(result).isEqualByComparingTo(new BigDecimal("456.78"));
    }

    @Test
    void getMyAccountBalance_shouldThrowServiceException_whenPayloadHasNoBalance() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createOkResponse("{\"otherField\": 1}");

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountBalance(eq(accountId), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(mockResponse);

        ExceptionTestUtil.assertThrowsServiceException(
                () -> this.accountService.getMyAccountBalance(accountId, null, null),
                "Malformed balance payload: missing balance");
    }

    @Test
    void getMyAccountBalance_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.getMyAccountBalance("1", null, null));
    }

    @Test
    void getMyAccountBalance_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountBalance(eq(accountId), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.accountService.getMyAccountBalance(accountId, null, null));
    }

    @Test
    void getMyAccountBalance_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountBalance(eq(accountId), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(
                () -> this.accountService.getMyAccountBalance(accountId, null, null), this.authService);
    }

    @Test
    void getMyAccountPayments_shouldReturnPayments_whenAccountExists() {
        final var accountId = "1";
        final var payment = MockDataUtil.createPaymentResponseDto();
        final var expected = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountPayments(eq(accountId), any(), any(), any(), any(), any(),
                eq(TestUtil.TEST_AUTH_HEADER))).thenReturn(expected);

        final var result = this.accountService.getMyAccountPayments(accountId, null, null, null, 0, 20);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyAccountPayments_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.accountService.getMyAccountPayments("1", null, null, null, 0, 20));
    }

    @Test
    void getMyAccountPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountPayments(eq(accountId), any(), any(), any(), any(), any(),
                eq(TestUtil.TEST_AUTH_HEADER))).thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.accountService.getMyAccountPayments(accountId, null, null, null, 0, 20));
    }

    @Test
    void getMyAccountPayments_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountPayments(eq(accountId), any(), any(), any(), any(), any(),
                eq(TestUtil.TEST_AUTH_HEADER))).thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(
                () -> this.accountService.getMyAccountPayments(accountId, null, null, null, 0, 20), this.authService);
    }

    @Test
    void getMyAccountOutgoingPayments_shouldReturnPayments_whenAccountExists() {
        final var accountId = "1";
        final var payment = MockDataUtil.createPaymentResponseDto();
        final var expected = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(expected);

        final var result = this.accountService.getMyAccountOutgoingPayments(accountId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyAccountOutgoingPayments_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.getMyAccountOutgoingPayments("1"));
    }

    @Test
    void getMyAccountOutgoingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil
                .assertThrowsBackendConnectionFailed(() -> this.accountService.getMyAccountOutgoingPayments(accountId));
    }

    @Test
    void getMyAccountOutgoingPayments_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getMyAccountOutgoingPayments(accountId),
                this.authService);
    }

    @Test
    void getMyAccountIncomingPayments_shouldReturnPayments_whenAccountExists() {
        final var accountId = "1";
        final var payment = MockDataUtil.createPaymentResponseDto();
        final var expected = Collections.singletonList(payment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(expected);

        final var result = this.accountService.getMyAccountIncomingPayments(accountId);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyAccountIncomingPayments_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.getMyAccountIncomingPayments("1"));
    }

    @Test
    void getMyAccountIncomingPayments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil
                .assertThrowsBackendConnectionFailed(() -> this.accountService.getMyAccountIncomingPayments(accountId));
    }

    @Test
    void getMyAccountIncomingPayments_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.accountService.getMyAccountIncomingPayments(accountId),
                this.authService);
    }

    @Test
    void createMyAccountPayment_shouldReturnPayment_whenValid() {
        final var accountId = "1";
        final var payment = TestDataBuilder.createUniquePayment();
        final var createdPayment = MockDataUtil.createPaymentResponseDto();
        createdPayment.setPublicId("1");
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdPayment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.createMyAccountPayment(accountId, payment, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        final var result = this.accountService.createMyAccountPayment(accountId, payment);

        assertThat(result.getPublicId()).isEqualTo("1");
    }

    @Test
    void createMyAccountPayment_shouldThrowAuthenticationException_whenNotAuthenticated() {
        final var payment = TestDataBuilder.createUniquePayment();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.createMyAccountPayment("1", payment));
    }

    @Test
    void createMyAccountPayment_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        final var payment = TestDataBuilder.createUniquePayment();
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.createMyAccountPayment(accountId, payment, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.accountService.createMyAccountPayment(accountId, payment));
    }

    @Test
    void createMyAccountPayment_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        final var payment = TestDataBuilder.createUniquePayment();
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.createMyAccountPayment(accountId, payment, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(
                () -> this.accountService.createMyAccountPayment(accountId, payment), this.authService);
    }

    @Test
    void updateMyAccountPayment_shouldReturnPayment_whenValid() {
        final var accountId = "1";
        final var paymentId = "2";
        final var payment = TestDataBuilder.createUniquePayment();
        payment.setPublicId(paymentId);
        final var updatedPayment = MockDataUtil.createPaymentResponseDto();
        updatedPayment.setPublicId(paymentId);
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedPayment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateMyAccountPayment(accountId, paymentId, payment, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        final var result = this.accountService.updateMyAccountPayment(accountId, paymentId, payment);

        assertThat(result.getPublicId()).isEqualTo(paymentId);
    }

    @Test
    void updateMyAccountPayment_shouldThrowAuthenticationException_whenNotAuthenticated() {
        final var payment = TestDataBuilder.createUniquePayment();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.accountService.updateMyAccountPayment("1", "2", payment));
    }

    @Test
    void updateMyAccountPayment_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        final var paymentId = "2";
        final var payment = TestDataBuilder.createUniquePayment();
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateMyAccountPayment(accountId, paymentId, payment, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.accountService.updateMyAccountPayment(accountId, paymentId, payment));
    }

    @Test
    void updateMyAccountPayment_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        final var paymentId = "2";
        final var payment = TestDataBuilder.createUniquePayment();
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateMyAccountPayment(accountId, paymentId, payment, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(
                () -> this.accountService.updateMyAccountPayment(accountId, paymentId, payment), this.authService);
    }

    @Test
    void updateUserAccountMeta_shouldSucceed_whenValid() {
        final var accountId = "1";
        final var userId = "2";
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateUserAccountMeta(eq(accountId), eq(userId), Mockito.any(UserAccountMetaDto.class),
                eq(TestUtil.TEST_AUTH_HEADER))).thenReturn(mockResponse);

        this.accountService.updateUserAccountMeta(accountId, userId, true, false);

        verify(this.accountClient).updateUserAccountMeta(eq(accountId), eq(userId),
                Mockito.any(UserAccountMetaDto.class), eq(TestUtil.TEST_AUTH_HEADER));
    }

    @Test
    void updateUserAccountMeta_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.accountService.updateUserAccountMeta("1", "2", true, false));
    }

    @Test
    void updateUserAccountMeta_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var accountId = "1";
        final var userId = "2";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateUserAccountMeta(eq(accountId), eq(userId), Mockito.any(UserAccountMetaDto.class),
                eq(TestUtil.TEST_AUTH_HEADER))).thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.accountService.updateUserAccountMeta(accountId, userId, true, false));
    }

    @Test
    void updateUserAccountMeta_shouldThrowAuthenticationException_when401Error() {
        final var accountId = "1";
        final var userId = "2";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateUserAccountMeta(eq(accountId), eq(userId), Mockito.any(UserAccountMetaDto.class),
                eq(TestUtil.TEST_AUTH_HEADER))).thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(
                () -> this.accountService.updateUserAccountMeta(accountId, userId, true, false), this.authService);
    }

    @Test
    void getMyAccountBalance_shouldThrowServiceException_whenForbidden() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createMockResponse(403);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountBalance(eq(accountId), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(mockResponse);

        ExceptionTestUtil.assertThrowsServiceException(
                () -> this.accountService.getMyAccountBalance(accountId, null, null), "Access denied to this account");
    }

    @Test
    void getMyAccountBalance_shouldThrowServiceException_whenNon200AndNon403() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createMockResponse(500);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getMyAccountBalance(eq(accountId), any(), any(), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(mockResponse);

        assertThatThrownBy(() -> this.accountService.getMyAccountBalance(accountId, null, null))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void getAccountBalance_shouldThrowServiceException_whenNon200Response() {
        final var accountId = "1";
        final var mockResponse = ResponseTestUtil.createMockResponse(500);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getAccountBalance(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        assertThatThrownBy(() -> this.accountService.getAccountBalance(accountId)).isInstanceOf(ServiceException.class);
    }

    @Test
    void deleteMyAccountPayment_shouldReturnTrue_whenDeletionSuccessful() {
        final var accountId = "1";
        final var paymentId = "2";
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.deleteMyAccountPayment(accountId, paymentId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        assertThat(this.accountService.deleteMyAccountPayment(accountId, paymentId)).isTrue();
    }

    @Test
    void deleteMyAccountPayment_shouldReturnFalse_whenDeletionFails() {
        final var accountId = "1";
        final var paymentId = "2";
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.deleteMyAccountPayment(accountId, paymentId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        assertThat(this.accountService.deleteMyAccountPayment(accountId, paymentId)).isFalse();
    }

    @Test
    void deleteMyAccountPayment_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil
                .assertThrowsAuthenticationRequired(() -> this.accountService.deleteMyAccountPayment("1", "2"));
    }

    @Test
    void getOutgoingPayments_shouldThrowServiceException_whenNon200Response() {
        final var accountId = "1";
        final var response = Mockito.mock(Response.class);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getOutgoingPayments(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        assertThatThrownBy(() -> this.accountService.getOutgoingPayments(accountId))
                .isInstanceOf(ServiceException.class);
        verify(response).close();
    }

    @Test
    void getIncomingPayments_shouldThrowServiceException_whenNon200Response() {
        final var accountId = "1";
        final var response = Mockito.mock(Response.class);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.getIncomingPayments(accountId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        assertThatThrownBy(() -> this.accountService.getIncomingPayments(accountId))
                .isInstanceOf(ServiceException.class);
        verify(response).close();
    }

    @Test
    void updateUserAccountMeta_shouldThrowServiceException_whenNon204Response() {
        final var accountId = "1";
        final var userId = "2";
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.updateUserAccountMeta(eq(accountId), eq(userId), Mockito.any(UserAccountMetaDto.class),
                eq(TestUtil.TEST_AUTH_HEADER))).thenReturn(mockResponse);

        assertThatThrownBy(() -> this.accountService.updateUserAccountMeta(accountId, userId, true, false))
                .isInstanceOf(ServiceException.class).hasMessage("Failed to update user-account meta: HTTP 400");
    }

    @Test
    void createMyAccountPayment_shouldThrowServiceException_whenNon201Response() {
        final var accountId = "1";
        final var payment = TestDataBuilder.createUniquePayment();
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.accountClient.createMyAccountPayment(accountId, payment, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        assertThatThrownBy(() -> this.accountService.createMyAccountPayment(accountId, payment))
                .isInstanceOf(ServiceException.class).hasMessage("Failed to create payment: HTTP 400");
    }
}
