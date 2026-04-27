package de.vptr.midas.gui.service;

import static de.vptr.midas.gui.util.TestUtil.TEST_AUTH_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.UserGroupClient;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class UserGroupServiceTest {

    @RestClient
    @Inject
    @Mock
    UserGroupClient groupClient;

    @Mock
    AuthService authService;

    @InjectMocks
    UserGroupService userGroupService;

    @Test
    void getAllGroups_shouldReturnGroupList_whenAuthenticationProvided() {
        // Given
        final var authHeader = TEST_AUTH_HEADER;
        final var group1 = new UserGroupResponseDto();
        final var group2 = new UserGroupResponseDto();
        final var expectedGroups = Arrays.asList(group1, group2);

        when(this.groupClient.getAllGroups(0, 50, authHeader)).thenReturn(expectedGroups);

        // When
        final var result = this.userGroupService.getAllGroups(authHeader, 0, 50);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(group1, group2);
        verify(this.groupClient).getAllGroups(0, 50, authHeader);
    }

    @Test
    void getAllGroups_shouldThrowServiceException_whenAuthHeaderIsNull() {
        // Given: null auth header

        // When & Then: service call should throw exception
        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.userGroupService.getAllGroups(null, 0, 50));
    }

    @Test
    void getAllGroups_shouldReturnEmptyList_whenAuthHeaderIsEmpty() {
        // Given
        final List<UserGroupResponseDto> emptyList = Collections.emptyList();
        when(this.groupClient.getAllGroups(0, 50, "")).thenReturn(emptyList);

        // When
        final var result = this.userGroupService.getAllGroups("", 0, 50);

        // Then
        assertThat(result).isEmpty();
        verify(this.groupClient).getAllGroups(0, 50, "");
    }

    @Test
    void getAllGroups_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        // Given
        when(this.groupClient.getAllGroups(0, 50, TEST_AUTH_HEADER))
                .thenThrow(new ProcessingException("Connection failed"));

        // When & Then
        ExceptionTestUtil
                .assertThrowsBackendConnectionFailed(() -> this.userGroupService.getAllGroups(TEST_AUTH_HEADER, 0, 50));
    }

    @Test
    void getAllGroups_shouldThrowAuthenticationException_when401Error() {
        // Given
        final Response mockResponse = ResponseTestUtil.createUnauthorizedResponse();
        final WebApplicationException webException = new WebApplicationException(mockResponse);

        when(this.groupClient.getAllGroups(0, 50, TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.userGroupService.getAllGroups(TEST_AUTH_HEADER, 0, 50),
                this.authService);
    }

    @Test
    void getGroupById_shouldReturnGroup_whenGroupExists() {
        // Given
        final var groupId = 1L;
        final var expectedGroup = new UserGroupResponseDto();
        expectedGroup.setId(groupId);
        expectedGroup.setName("Test Group");
        expectedGroup.setUserCount(5L);
        final Response mockResponse = ResponseTestUtil.createOkResponse(expectedGroup);

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.getGroup(groupId, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userGroupService.getGroupById(groupId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(groupId);
        assertThat(result.get().getName()).isEqualTo("Test Group");
    }

    @Test
    void getGroupById_shouldReturnEmpty_whenGroupNotFound() {
        // Given
        final var groupId = 999L;
        final Response mockResponse = ResponseTestUtil.createNotFoundResponse();

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.getGroup(groupId, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userGroupService.getGroupById(groupId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getUsersInGroup_shouldReturnUserList_whenGroupExists() {
        // Given
        final var groupId = 1L;
        final var user1 = MockDataUtil.createUserDto();
        user1.setId(1L);
        user1.setUsername("user1");
        final var user2 = MockDataUtil.createUserDto();
        user2.setId(2L);
        user2.setUsername("user2");
        final var expectedUsers = Arrays.asList(user1, user2);

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.getUsersInGroup(groupId, TEST_AUTH_HEADER)).thenReturn(expectedUsers);

        // When
        final var result = this.userGroupService.getUsersInGroup(groupId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(user1, user2);
        verify(this.groupClient).getUsersInGroup(groupId, TEST_AUTH_HEADER);
    }

    @Test
    void createGroup_shouldReturnCreatedGroup_whenValidGroup() {
        // Given
        final var newGroup = MockDataUtil.createUserGroupDto();
        final var createdGroup = new UserGroupResponseDto();
        createdGroup.setId(1L);
        createdGroup.setName(MockDataUtil.TestConstants.GROUP_NAME);
        createdGroup.setUserCount(1L);
        final Response mockResponse = ResponseTestUtil.createCreatedResponse(createdGroup);

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.createGroup(newGroup, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userGroupService.createGroup(newGroup);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(MockDataUtil.TestConstants.GROUP_NAME);
        verify(this.groupClient).createGroup(newGroup, TEST_AUTH_HEADER);
    }

    @Test
    void createGroup_shouldThrowServiceException_whenCreationFails() {
        // Given
        final var newGroup = MockDataUtil.createUserGroupDto();
        final Response mockResponse = ResponseTestUtil.createBadRequestResponse();

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.createGroup(newGroup, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.userGroupService.createGroup(newGroup))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void updateGroup_shouldReturnUpdatedGroup_whenValidGroup() {
        // Given
        final var groupToUpdate = MockDataUtil.createUserGroupDto();
        groupToUpdate.setId(1L);
        groupToUpdate.setName("Updated Group");
        final var updatedGroup = new UserGroupResponseDto();
        updatedGroup.setId(1L);
        updatedGroup.setName("Updated Group");
        updatedGroup.setUserCount(2L);
        final Response mockResponse = ResponseTestUtil.createOkResponse(updatedGroup);

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.updateGroup(1L, groupToUpdate, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userGroupService.updateGroup(groupToUpdate);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Updated Group");
        verify(this.groupClient).updateGroup(1L, groupToUpdate, TEST_AUTH_HEADER);
    }

    @Test
    void deleteGroup_shouldReturnTrue_whenDeletionSuccessful() {
        // Given
        final var groupId = 1L;
        final Response mockResponse = ResponseTestUtil.createNoContentResponse();

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.deleteGroup(groupId, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.userGroupService.deleteGroup(groupId);

        // Then
        assertThat(result).isTrue();
        verify(this.groupClient).deleteGroup(groupId, TEST_AUTH_HEADER);
    }

    @Test
    void deleteGroup_shouldReturnFalse_whenDeletionFails() {
        // Given
        final var groupId = 1L;
        final Response mockResponse = ResponseTestUtil.createBadRequestResponse();

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.deleteGroup(groupId, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.userGroupService.deleteGroup(groupId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void addUserToGroup_shouldReturnTrue_whenSuccessful() {
        // Given
        final var groupId = 1L;
        final var userId = 1L;
        final Response mockResponse = ResponseTestUtil.createCreatedResponse(null);

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.addUserToGroup(groupId, userId, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.userGroupService.addUserToGroup(groupId, userId);

        // Then
        assertThat(result).isTrue();
        verify(this.groupClient).addUserToGroup(groupId, userId, TEST_AUTH_HEADER);
    }

    @Test
    void removeUserFromGroup_shouldReturnTrue_whenSuccessful() {
        // Given
        final var groupId = 1L;
        final var userId = 1L;
        final Response mockResponse = ResponseTestUtil.createNoContentResponse();

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.removeUserFromGroup(groupId, userId, TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.userGroupService.removeUserFromGroup(groupId, userId);

        // Then
        assertThat(result).isTrue();
        verify(this.groupClient).removeUserFromGroup(groupId, userId, TEST_AUTH_HEADER);
    }

    @Test
    void getGroupById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userGroupService.getGroupById(1L));
    }

    @Test
    void createGroup_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var newGroup = MockDataUtil.createUserGroupDto();
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userGroupService.createGroup(newGroup));
    }

    @Test
    void getAllGroups_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        // Given
        when(this.groupClient.getAllGroups(0, 50, TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil
                .assertThrowsUnexpectedError(() -> this.userGroupService.getAllGroups(TEST_AUTH_HEADER, 0, 50));
    }

    @Test
    void getGroupById_shouldThrowServiceException_whenWebApplicationExceptionOccurs() {
        // Given
        final var groupId = 1L;
        final Response mockResponse = ResponseTestUtil.createInternalServerErrorResponse();
        final WebApplicationException webException = new WebApplicationException(mockResponse);

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.getGroup(groupId, TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertThrowsBackendError(() -> this.userGroupService.getGroupById(groupId), 500);
    }

    @Test
    void getUsersInGroup_shouldReturnEmptyList_whenGroupHasNoUsers() {
        // Given
        final var groupId = 1L;
        final List<UserDto> emptyList = Collections.emptyList();

        when(this.authService.getAuthorizationHeader()).thenReturn(TEST_AUTH_HEADER);
        when(this.groupClient.getUsersInGroup(groupId, TEST_AUTH_HEADER)).thenReturn(emptyList);

        // When
        final var result = this.userGroupService.getUsersInGroup(groupId);

        // Then
        assertThat(result).isEmpty();
    }
}
