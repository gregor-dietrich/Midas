package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import de.vptr.midas.gui.client.UserRankClient;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractSessionServiceTestBase;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.ws.rs.WebApplicationException;

@SuppressWarnings("PMD.LinguisticNaming")
class UserRankServiceTest extends AbstractSessionServiceTestBase {

    @Mock
    UserRankClient userRankClient;

    @Mock
    AuthService authService;

    @Mock
    UserService userService;

    @InjectMocks
    UserRankService userRankService;

    @Test
    void getAllRanks_shouldReturnRankList_whenAuthenticationProvided() {
        // Given
        final var rank1 = MockDataUtil.createUserRankResponseDto();
        rank1.setName("Admin");
        final var rank2 = MockDataUtil.createUserRankResponseDto();
        rank2.setName("User");
        rank2.setId(2L);
        final var expectedRanks = Arrays.asList(rank1, rank2);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getAllRanks(TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedRanks);

        // When
        final var result = this.userRankService.getAllRanks();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(rank1, rank2);
        verify(this.userRankClient).getAllRanks(TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getAllRanks_shouldThrowAuthenticationException_whenAuthHeaderUnavailable() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.userRankService.getAllRanks());
    }

    @Test
    void getAllRanks_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getAllRanks(TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        // When & Then
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.userRankService.getAllRanks());
    }

    @Test
    void getAllRanks_shouldThrowAuthenticationException_when401Error() {
        // Given
        final WebApplicationException webException = ExceptionTestUtil.create401Exception();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getAllRanks(TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.userRankService.getAllRanks(), this.authService);
    }

    @Test
    void getCurrentUserRank_shouldReturnCachedRank_whenCacheIsValid() {
        // Given
        final var cachedRank = MockDataUtil.createUserRankResponseDto();
        final var cachedAt = System.currentTimeMillis();
        when(this.vaadinSession.getAttribute("current.user.rank")).thenReturn(cachedRank);
        when(this.vaadinSession.getAttribute("current.user.rank.timestamp")).thenReturn(cachedAt);

        // When
        final var result = this.userRankService.getCurrentUserRank();

        // Then
        assertThat(result).isSameAs(cachedRank);
        verifyNoInteractions(this.userService);
        verifyNoInteractions(this.userRankClient);
    }

    @Test
    void getCurrentUserRank_shouldClearCorruptedCache_whenSessionAttributesHaveUnexpectedTypes() {
        // Given
        when(this.vaadinSession.getAttribute("current.user.rank")).thenReturn("corrupted");
        when(this.vaadinSession.getAttribute("current.user.rank.timestamp")).thenReturn("corrupted");
        when(this.userService.getCurrentUser()).thenReturn(Optional.empty());

        // When
        final var result = this.userRankService.getCurrentUserRank();

        // Then
        assertThat(result.getId()).isNull();
        verify(this.vaadinSession).setAttribute("current.user.rank", null);
        verify(this.vaadinSession).setAttribute("current.user.rank.timestamp", null);
    }

    @Test
    void getRankById_shouldReturnRank_whenRankExists() {
        // Given
        final var rankId = 1L;
        final var expectedRank = MockDataUtil.createUserRankResponseDto();
        expectedRank.setName("Test Rank");
        expectedRank.setId(rankId);
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedRank);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getRank(rankId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userRankService.getRankById(rankId);

        // Then
        assertThat(result.getId()).isEqualTo(rankId);
        assertThat(result.getName()).isEqualTo("Test Rank");
        assertThat(result.isUserAdd()).isTrue();
        assertThat(result.isPostEdit()).isTrue();
    }

    @Test
    void getRankById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var rankId = 1L;
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.userRankService.getRankById(rankId));
    }

    @Test
    void getRankById_shouldThrowServiceException_whenRankNotFound() {
        // Given
        final var rankId = 999L;
        final var mockResponse = ResponseTestUtil.createNotFoundResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getRank(rankId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        ExceptionTestUtil.assertThrowsServiceException(() -> this.userRankService.getRankById(rankId),
                "User rank not found");
    }

    @Test
    void createRank_shouldReturnCreatedRank_whenValidRank() {
        // Given
        final var newRank = MockDataUtil.createUserRankDto();
        newRank.setName("New Rank");
        final var createdRank = MockDataUtil.createUserRankDto();
        createdRank.setId(1L);
        createdRank.setName("New Rank");
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdRank);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.createRank(newRank, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userRankService.createRank(newRank);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New Rank");
        assertThat(result.isUserAdd()).isTrue();
        verify(this.userRankClient).createRank(newRank, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void createRank_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var newRank = MockDataUtil.createUserRankDto();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.userRankService.createRank(newRank));
    }

    @Test
    void createRank_shouldThrowServiceException_whenCreationFails() {
        // Given
        final var newRank = MockDataUtil.createUserRankDto();
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.createRank(newRank, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.userRankService.createRank(newRank)).isInstanceOf(ServiceException.class);
    }

    @Test
    void updateRank_shouldReturnUpdatedRank_whenValidRank() {
        // Given
        final var rankToUpdate = MockDataUtil.createUserRankDto();
        rankToUpdate.setName("Updated Rank");
        rankToUpdate.setId(1L);
        final var mockResponse = ResponseTestUtil.createOkResponse(rankToUpdate);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.updateRank(1L, rankToUpdate, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.userRankService.updateRank(rankToUpdate);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Updated Rank");
        assertThat(result.isUserEdit()).isTrue();
        verify(this.userRankClient).updateRank(1L, rankToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void updateRank_shouldThrowServiceException_whenUpdateFails() {
        // Given
        final var rankToUpdate = MockDataUtil.createUserRankDto();
        rankToUpdate.setName("Updated Rank");
        rankToUpdate.setId(1L);
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.updateRank(1L, rankToUpdate, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.userRankService.updateRank(rankToUpdate)).isInstanceOf(ServiceException.class);
    }

    @Test
    void deleteRank_shouldSucceed_whenDeletionSuccessful() {
        // Given
        final var rankId = 1L;
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.deleteRank(rankId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        this.userRankService.deleteRank(rankId);

        // Then
        verify(this.userRankClient).deleteRank(rankId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteRank_shouldThrowServiceException_whenDeletionFails() {
        // Given
        final var rankId = 1L;
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.deleteRank(rankId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.userRankService.deleteRank(rankId)).isInstanceOf(ServiceException.class);
    }

    @Test
    void deleteRank_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var rankId = 1L;
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.userRankService.deleteRank(rankId));
    }

    @Test
    void getRankById_shouldThrowAuthenticationException_when401ErrorOnFetch() {
        // Given
        final var rankId = 1L;
        final WebApplicationException webException = ExceptionTestUtil.create401Exception();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getRank(rankId, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertThrowsSessionExpired(() -> this.userRankService.getRankById(rankId));
    }

    @Test
    void createRank_shouldThrowAuthenticationException_when401ErrorOnCreate() {
        // Given
        final var newRank = MockDataUtil.createUserRankDto();
        final WebApplicationException webException = ExceptionTestUtil.create401Exception();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.createRank(newRank, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertThrowsSessionExpired(() -> this.userRankService.createRank(newRank));
    }

    @Test
    void getAllRanks_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getAllRanks(TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.userRankService.getAllRanks());
    }

    @Test
    void getRankById_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        // Given
        final var rankId = 1L;
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.userRankClient.getRank(rankId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.userRankService.getRankById(rankId));
    }

    @Test
    void clearCachedUserRank_shouldRemoveCachedValuesFromSession() {
        // When
        this.userRankService.clearCachedUserRank();

        // Then
        verify(this.vaadinSession).setAttribute("current.user.rank", null);
        verify(this.vaadinSession).setAttribute("current.user.rank.timestamp", null);
    }
}
