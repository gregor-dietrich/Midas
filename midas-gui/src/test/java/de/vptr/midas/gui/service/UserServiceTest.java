package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.UserClient;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestDataBuilder;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.inject.Inject;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @RestClient
    @Inject
    @Mock
    UserClient userClient;

    @Mock
    AuthService authService;

    @InjectMocks
    UserService userService;

    @Test
    void getAllUsers_shouldReturnUserList_whenAuthenticationProvided() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final var user1 = MockDataUtil.createUserResponseDto(1L, "user1", "user1@example.com");
        final var user2 = MockDataUtil.createUserResponseDto(2L, "user2", "user2@example.com");
        final var expectedUsers = Arrays.asList(user1, user2);

        when(this.userClient.getAllUsers(0, 50, authHeader)).thenReturn(expectedUsers);

        // When
        final var result = this.userService.getAllUsers(authHeader, 0, 50);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(user1, user2);
        verify(this.userClient).getAllUsers(0, 50, authHeader);
    }

    @Test
    void getAllUsers_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userService.getAllUsers(null, 0, 50));
    }

    @Test
    void getAllUsers_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        when(this.userClient.getAllUsers(0, 50, authHeader))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        // When & Then
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.userService.getAllUsers(authHeader, 0, 50));
    }

    @Test
    void getAllUsers_shouldThrowAuthenticationException_when401Error() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        when(this.userClient.getAllUsers(0, 50, authHeader)).thenThrow(ExceptionTestUtil.create401Exception());

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.userService.getAllUsers(authHeader, 0, 50),
                this.authService);
    }

    @Test
    void getCurrentUser_shouldReturnUser_whenAuthenticated() {
        // Given
        final var expectedUser = MockDataUtil.createUserResponseDto(1L, "currentuser", "current@example.com");
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedUser);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.getCurrentUser(TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userService.getCurrentUser();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("currentuser");
        assertThat(result.get().getEmail()).isEqualTo("current@example.com");
    }

    @Test
    void getCurrentUser_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_shouldReturnEmpty_whenUserNotFound() {
        // Given
        final var mockResponse = ResponseTestUtil.createNotFoundResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.getCurrentUser(TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userService.getCurrentUser();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        // Given
        final var userId = 1L;
        final var expectedUser = MockDataUtil.createUserResponseDto(userId, "testuser", "test@example.com");
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedUser);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.getUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userService.getUserById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserById_shouldReturnEmpty_whenUserNotFound() {
        // Given
        final var userId = 999L;

        ServiceTestUtil.setupAuthHeader(this.authService);
        ServiceTestUtil.testReturnEmptyOn404(
                () -> when(this.userClient.getUser(userId, TestUtil.TEST_AUTH_HEADER))
                        .thenReturn(ResponseTestUtil.createNotFoundResponse()),
                () -> this.userService.getUserById(userId));
    }

    @Test
    void getUserByUsername_shouldReturnUser_whenUserExists() {
        // Given
        final String username = "testuser";
        final var expectedUser = MockDataUtil.createUserResponseDto(1L, username, "test@example.com");
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedUser);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.getUserByUsername(username, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userService.getUserByUsername(username);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(username);
    }

    @Test
    void getUserByUsername_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.userService.getUserByUsername("testuser"));
    }

    @Test
    void createUser_shouldReturnCreatedUser_whenValidUser() {
        // Given
        final var newUser = TestDataBuilder.createUser("newuser", "new@example.com");
        newUser.setPassword("password123");
        final var createdUser = MockDataUtil.createUserResponseDto(1L, "newuser", "new@example.com");
        createdUser.setActivated(false);
        createdUser.setBanned(false);
        createdUser.setCreated(LocalDateTime.now());
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdUser);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.createUser(newUser, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userService.createUser(newUser);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(this.userClient).createUser(newUser, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void createUser_shouldThrowServiceException_whenCreationFails() {
        // Given
        final var newUser = TestDataBuilder.createUser("newuser", "new@example.com");

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.createUser(newUser, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(ResponseTestUtil.createBadRequestResponse());
        assertThatThrownBy(() -> this.userService.createUser(newUser))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenValidUser() {
        // Given
        final var userToUpdate = TestDataBuilder.createUser("updateduser", "updated@example.com");
        userToUpdate.setId(1L);
        userToUpdate.setActivated(true);
        final var updatedUser = MockDataUtil.createUserResponseDto(1L, "updateduser", "updated@example.com");
        updatedUser.setActivated(true);
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedUser);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.updateUser(1L, userToUpdate, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userService.updateUser(userToUpdate);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("updateduser");
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        verify(this.userClient).updateUser(1L, userToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void updateUser_shouldThrowServiceException_whenUpdateFails() {
        // Given
        final var userToUpdate = TestDataBuilder.createUser("updateduser", "updated@example.com");
        userToUpdate.setId(1L);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.updateUser(1L, userToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(ResponseTestUtil.createBadRequestResponse());
        assertThatThrownBy(() -> this.userService.updateUser(userToUpdate))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void deleteUser_shouldReturnTrue_whenDeletionSuccessful() {
        // Given
        final var userId = 1L;
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.deleteUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.userService.deleteUser(userId);

        // Then
        assertThat(result).isTrue();
        verify(this.userClient).deleteUser(userId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteUser_shouldReturnFalse_whenDeletionFails() {
        // Given
        final var userId = 1L;

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.deleteUser(userId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(ResponseTestUtil.createBadRequestResponse());

        // When
        final boolean result = this.userService.deleteUser(userId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getUserById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userService.getUserById(1L));
    }

    @Test
    void createUser_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var newUser = TestDataBuilder.createUser("newuser", "new@example.com");
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userService.createUser(newUser));
    }

    @Test
    void getAllUsers_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        when(this.userClient.getAllUsers(0, 50, authHeader))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.userService.getAllUsers(authHeader, 0, 50));
    }

    @Test
    void getUserById_shouldThrowServiceException_whenWebApplicationExceptionOccurs() {
        // Given
        final var userId = 1L;

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.getUser(userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createWebApplicationException(500));

        // When & Then
        ExceptionTestUtil.assertThrowsServiceException(() -> this.userService.getUserById(userId),
                "Backend error: 500");
    }

    @Test
    void getUserById_shouldUseBackendPayload_whenWebApplicationExceptionContainsBody() {
        final var userId = 1L;
        final var response = ResponseTestUtil.createMockResponse(500, "{\"message\":\"User lookup failed\"}");

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.getUser(userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new jakarta.ws.rs.WebApplicationException(response));

        ExceptionTestUtil.assertThrowsServiceException(() -> this.userService.getUserById(userId),
                "User lookup failed");
    }

    @Test
    void getCurrentUser_shouldThrowAuthenticationException_when401Error() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.getCurrentUser(TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.userService.getCurrentUser(), this.authService);
    }

    @Test
    void getAllUsers_shouldReturnEmptyList_whenNoUsersExist() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final List<UserResponseDto> emptyList = Collections.emptyList();

        when(this.userClient.getAllUsers(0, 50, authHeader)).thenReturn(emptyList);

        // When
        final var result = this.userService.getAllUsers(authHeader, 0, 50);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateUser_shouldHandleServiceExceptionProperly() {
        // Given
        final var userToUpdate = TestDataBuilder.createUser("updateduser", "updated@example.com");
        userToUpdate.setId(1L);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.updateUser(1L, userToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.userService.updateUser(userToUpdate));
    }

    @Test
    void updateUserPassword_shouldReturnUpdatedUser_whenValidPassword() {
        // Given
        final var userToUpdate = new UserDto();
        userToUpdate.setId(1L);
        userToUpdate.setPassword("newPassword123");
        final var updatedUser = MockDataUtil.createUserResponseDto(1L, "testuser", "test@example.com");
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedUser);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.patchUser(1L, userToUpdate, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userService.updateUserPassword(userToUpdate);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(this.userClient).patchUser(1L, userToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void updateUserPassword_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var userToUpdate = new UserDto();
        userToUpdate.setId(1L);
        userToUpdate.setPassword("newPassword123");

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userService.updateUserPassword(userToUpdate));
    }

    @Test
    void updateUserPassword_shouldThrowServiceException_whenUpdateFails() {
        // Given
        final var userToUpdate = new UserDto();
        userToUpdate.setId(1L);
        userToUpdate.setPassword("newPassword123");
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userClient.patchUser(1L, userToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(ResponseTestUtil.createBadRequestResponse());
        assertThatThrownBy(() -> this.userService.updateUserPassword(userToUpdate))
                .isInstanceOf(ServiceException.class);
    }
}
