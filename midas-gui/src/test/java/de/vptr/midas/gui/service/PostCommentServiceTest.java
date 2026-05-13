package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PostCommentClient;
import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.enterprise.event.Event;

@SuppressWarnings({ "NullAway", "PMD.LinguisticNaming", "microprofile-restclient" })
@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock
    @RestClient
    PostCommentClient commentClient;

    @Mock
    AuthService authService;

    @Mock
    Event<String> commentEvent;

    @InjectMocks
    PostCommentService postCommentService;

    @Test
    void getAllComments_shouldReturnCommentList_whenAuthenticationProvided() {
        // Given
        final var comment1 =
                MockDataUtil.createPostCommentResponseDto("1", "First comment", "1", "Test Post", "1", "testuser");
        final var comment2 =
                MockDataUtil.createPostCommentResponseDto("2", "Second comment", "1", "Test Post", "1", "testuser");
        final var expectedComments = Arrays.asList(comment1, comment2);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getAllComments(0, 50, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedComments);

        // When
        final var result = this.postCommentService.getAllComments(0, 50);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(comment1, comment2);
        verify(this.commentClient).getAllComments(0, 50, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getAllComments_shouldThrowAuthenticationException_whenAuthHeaderUnavailable() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCommentService.getAllComments(0, 50));
    }

    @Test
    void getAllComments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getAllComments(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        // When & Then
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.postCommentService.getAllComments(0, 50));
    }

    @Test
    void getAllComments_shouldThrowAuthenticationException_when401Error() {
        // Given
        final var webException = ExceptionTestUtil.create401Exception();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getAllComments(0, 50, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.postCommentService.getAllComments(0, 50),
                this.authService);
    }

    @Test
    void getCommentsByPost_shouldReturnComments_whenPostExists() {
        // Given
        final var postId = "1";

        final var comment =
                MockDataUtil.createPostCommentResponseDto("1", "Test comment", postId, "Test Post", "1", "testuser");
        final var expectedComments = Collections.singletonList(comment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByPost(postId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedComments);

        // When
        final var result = this.postCommentService.getCommentsByPost(postId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Test comment");
        verify(this.commentClient).getCommentsByPost(postId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getCommentsByPost_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var postId = "1";
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.postCommentService.getCommentsByPost(postId));
    }

    @Test
    void getCommentById_shouldReturnComment_whenCommentExists() {
        // Given
        final var commentId = "1";
        final var user = MockDataUtil.createUserDto();
        final var post = MockDataUtil.createPostDto();
        post.setPublicId("1");
        final var expectedComment = MockDataUtil.createPostCommentResponseDto(commentId, "Test comment",
                post.getPublicId(), post.getTitle(), user.getPublicId(), user.getUsername());
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedComment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getComment(commentId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postCommentService.getCommentById(commentId);

        // Then
        assertThat(result.getPublicId()).isEqualTo(commentId);
        assertThat(result.getContent()).isEqualTo("Test comment");
    }

    @Test
    void getCommentById_shouldThrowServiceException_whenCommentNotFound() {
        // Given
        final var commentId = "999";
        final var mockResponse = ResponseTestUtil.createNotFoundResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getComment(commentId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        ExceptionTestUtil.assertThrowsServiceException(() -> this.postCommentService.getCommentById(commentId),
                "Comment not found");
    }

    @Test
    void createComment_shouldReturnCreatedComment_whenValidComment() {
        // Given
        final var user = MockDataUtil.createUserDto();
        final var post = MockDataUtil.createPostDto();
        post.setPublicId("1");
        final var newComment = MockDataUtil.createPostCommentDto();
        newComment.setPublicId(null);
        newComment.setContent("New comment");
        newComment.setPostPublicId("1");
        newComment.setPostTitle(post.getTitle());
        newComment.setUserPublicId(user.getPublicId());
        newComment.setUsername(user.getUsername());
        newComment.setCreated(null);
        final var createdComment = MockDataUtil.createPostCommentResponseDto("1", "New comment", post.getPublicId(),
                post.getTitle(), user.getPublicId(), user.getUsername());
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdComment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.createComment(any(PostCommentDto.class), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(mockResponse);

        // When
        final var result = this.postCommentService.createComment(newComment);

        // Then
        assertThat(result.getPublicId()).isEqualTo("1");
        assertThat(result.getContent()).isEqualTo("New comment");
        verify(this.commentClient).createComment(any(PostCommentDto.class), eq(TestUtil.TEST_AUTH_HEADER));
    }

    @Test
    void createComment_shouldThrowServiceException_whenCreationFails() {
        // Given
        final var newComment = MockDataUtil.createPostCommentDto();
        newComment.setContent("New comment");
        newComment.setPostPublicId("1");
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.createComment(any(PostCommentDto.class), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.postCommentService.createComment(newComment))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void updateComment_shouldReturnUpdatedComment_whenValidComment() {
        // Given
        final var user = MockDataUtil.createUserDto();
        final var post = MockDataUtil.createPostDto();
        post.setPublicId("1");
        final var commentToUpdate = MockDataUtil.createPostCommentDto();
        commentToUpdate.setPublicId("1");
        commentToUpdate.setContent("Updated comment");
        commentToUpdate.setPostPublicId(post.getPublicId());
        commentToUpdate.setPostTitle(post.getTitle());
        commentToUpdate.setUserPublicId(user.getPublicId());
        commentToUpdate.setUsername(user.getUsername());
        commentToUpdate.setCreated(LocalDateTime.now(ZoneId.systemDefault()));
        final var updatedComment = MockDataUtil.createPostCommentResponseDto("1", "Updated comment", post.getPublicId(),
                post.getTitle(), user.getPublicId(), user.getUsername());
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedComment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.updateComment(eq("1"), any(PostCommentDto.class), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(mockResponse);

        // When
        final var result = this.postCommentService.updateComment(commentToUpdate);

        // Then
        assertThat(result.getPublicId()).isEqualTo("1");
        assertThat(result.getContent()).isEqualTo("Updated comment");
        verify(this.commentClient).updateComment(eq("1"), any(PostCommentDto.class), eq(TestUtil.TEST_AUTH_HEADER));
    }

    @Test
    void deleteComment_shouldSucceed_whenDeletionSuccessful() {
        // Given
        final var commentId = "1";
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.deleteComment(commentId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        this.postCommentService.deleteComment(commentId);

        // Then
        verify(this.commentClient).deleteComment(commentId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteComment_shouldThrowServiceException_whenDeletionFails() {
        // Given
        final var commentId = "1";
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.deleteComment(commentId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.postCommentService.deleteComment(commentId)).isInstanceOf(ServiceException.class);
    }

    @Test
    void getCommentsByPost_shouldThrowServiceException_whenWebApplicationExceptionOccurs() {
        // Given
        final var postId = "1";
        final var webException = ExceptionTestUtil.createWebApplicationException(500);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByPost(postId, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertThrowsServiceException(() -> this.postCommentService.getCommentsByPost(postId),
                "Backend request failed");
    }

    @Test
    void getCommentsByPost_shouldThrowAuthenticationException_when401ErrorOnFetch() {
        // Given
        final var postId = "1";
        final var webException = ExceptionTestUtil.create401Exception();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByPost(postId, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertThrowsSessionExpired(() -> this.postCommentService.getCommentsByPost(postId));
    }

    @Test
    void getAllComments_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getAllComments(0, 50, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.postCommentService.getAllComments(0, 50));
    }

    // ===================== getCommentsByUser tests =====================

    @Test
    void getCommentsByUser_shouldReturnComments_whenUserExists() {
        final var userId = "1";
        final var comment =
                MockDataUtil.createPostCommentResponseDto("1", "User comment", "1", "Test Post", userId, "testuser");
        final var expected = Collections.singletonList(comment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.postCommentService.getCommentsByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("User comment");
    }

    @Test
    void getCommentsByUser_shouldThrowServiceException_whenAuthHeaderIsNull() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCommentService.getCommentsByUser("1"));
    }

    @Test
    void getCommentsByUser_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final var userId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByUser(userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.postCommentService.getCommentsByUser(userId));
    }

    @Test
    void getCommentsByUser_shouldThrowAuthenticationException_when401Error() {
        final var userId = "1";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByUser(userId, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.postCommentService.getCommentsByUser(userId),
                this.authService);
    }

    // ===================== searchComments tests =====================

    @Test
    void searchComments_shouldReturnComments_whenQueryProvided() {
        final String query = "test";
        final var comment =
                MockDataUtil.createPostCommentResponseDto("1", "Search result", "1", "Test Post", "1", "testuser");
        final var expected = Collections.singletonList(comment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.searchComments(query, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.postCommentService.searchComments(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Search result");
    }

    @Test
    void searchComments_shouldThrowAuthenticationException_whenAuthHeaderUnavailable() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCommentService.searchComments("test"));
    }

    @Test
    void searchComments_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final String query = "test";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.searchComments(query, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.postCommentService.searchComments(query));
    }

    @Test
    void searchComments_shouldThrowAuthenticationException_when401Error() {
        final String query = "test";
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.searchComments(query, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.postCommentService.searchComments(query),
                this.authService);
    }

    // ===================== getCommentsByDateRange tests =====================

    @Test
    void getCommentsByDateRange_shouldReturnComments_whenValidDates() {
        final LocalDate startDate = LocalDate.of(2023, 1, 1);
        final LocalDate endDate = LocalDate.of(2023, 12, 31);
        final var comment =
                MockDataUtil.createPostCommentResponseDto("1", "Date range comment", "1", "Test Post", "1", "testuser");
        final var expected = Collections.singletonList(comment);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByDateRange(startDate, endDate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(expected);

        final var result = this.postCommentService.getCommentsByDateRange(startDate, endDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Date range comment");
    }

    @Test
    void getCommentsByDateRange_shouldThrowAuthenticationException_whenAuthHeaderUnavailable() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCommentService
                .getCommentsByDateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)));
    }

    @Test
    void getCommentsByDateRange_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        final LocalDate startDate = LocalDate.of(2023, 1, 1);
        final LocalDate endDate = LocalDate.of(2023, 12, 31);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByDateRange(startDate, endDate, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(
                () -> this.postCommentService.getCommentsByDateRange(startDate, endDate));
    }

    @Test
    void getCommentsByDateRange_shouldThrowAuthenticationException_when401Error() {
        final LocalDate startDate = LocalDate.of(2023, 1, 1);
        final LocalDate endDate = LocalDate.of(2023, 12, 31);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.commentClient.getCommentsByDateRange(startDate, endDate, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.create401Exception());

        ExceptionTestUtil.assertHandles401WithLogout(
                () -> this.postCommentService.getCommentsByDateRange(startDate, endDate), this.authService);
    }
}
