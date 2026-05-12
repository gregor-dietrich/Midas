package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PostClient;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

@SuppressWarnings({ "NullAway", "PMD.LinguisticNaming" })
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    PostClient postClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PostService postService;

    @Test
    void getAllPosts_shouldReturnPosts_whenAuthenticated() {
        // Given
        final var expectedPosts =
                Arrays.asList(MockDataUtil.createPostResponseDto(), MockDataUtil.createPostResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getAllPosts(0, 20, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getAllPosts();

        // Then
        assertThat(result).isEqualTo(expectedPosts);
        verify(this.postClient).getAllPosts(0, 20, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getAllPosts_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getAllPosts());
    }

    @Test
    void getAllPosts_shouldThrowServiceException_whenProcessingException() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getAllPosts(0, 20, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new ProcessingException("Connection failed"));

        // When & Then
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.postService.getAllPosts());
    }

    @Test
    void getAllPosts_shouldThrowAuthenticationException_when401() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        final WebApplicationException webAppException = new WebApplicationException(401);
        when(this.postClient.getAllPosts(0, 20, TestUtil.TEST_AUTH_HEADER)).thenThrow(webAppException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.postService.getAllPosts(), this.authService);
    }

    @Test
    void getPublishedPosts_shouldReturnPosts_whenAuthenticated() {
        // Given
        final var expectedPosts =
                Arrays.asList(MockDataUtil.createPostResponseDto(), MockDataUtil.createPostResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPublishedPosts(0, 20, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getPublishedPosts();

        // Then
        assertThat(result).isEqualTo(expectedPosts);
        verify(this.postClient).getPublishedPosts(0, 20, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getPublishedPosts_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getPublishedPosts());
    }

    @Test
    void getPostById_shouldReturnPost_whenPostExists() {
        // Given
        final var postId = 1L;
        final var expectedPost = MockDataUtil.createPostResponseDto();
        expectedPost.setId(postId);
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedPost);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPost(postId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postService.getPostById(postId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedPost);
    }

    @Test
    void getPostById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getPostById(1L));
    }

    @Test
    void getPostById_shouldReturnEmpty_whenPostNotFound() {
        // Given
        final var postId = 999L;
        final var mockResponse = ResponseTestUtil.createNotFoundResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPost(postId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postService.getPostById(postId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getPostsByUser_shouldReturnPosts_whenAuthenticated() {
        // Given
        final var userId = 1L;
        final var expectedPosts =
                Arrays.asList(MockDataUtil.createPostResponseDto(), MockDataUtil.createPostResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPostsByUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getPostsByUser(userId);

        // Then
        assertThat(result).isEqualTo(expectedPosts);
    }

    @Test
    void getPostsByUser_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getPostsByUser(1L));
    }

    @Test
    void getPostsByCategory_shouldReturnPosts_whenAuthenticated() {
        // Given
        final var categoryId = 1L;
        final var expectedPosts =
                Arrays.asList(MockDataUtil.createPostResponseDto(), MockDataUtil.createPostResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPostsByCategory(categoryId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getPostsByCategory(categoryId);

        // Then
        assertThat(result).isEqualTo(expectedPosts);
    }

    @Test
    void getPostsByCategory_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getPostsByCategory(1L));
    }

    @Test
    void createPost_shouldReturnCreatedPost_whenValid() {
        // Given
        final var newPost = MockDataUtil.createPostDto();
        final var createdPost = MockDataUtil.createPostDto();
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdPost);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.createPost(newPost, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postService.createPost(newPost);

        // Then
        assertThat(result).isEqualTo(createdPost);
        verify(this.postClient).createPost(newPost, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void createPost_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var newPost = MockDataUtil.createPostDto();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.postService.createPost(newPost));
    }

    @Test
    void createPost_shouldThrowServiceException_whenCreationFails() {
        // Given
        final var newPost = MockDataUtil.createPostDto();
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.createPost(newPost, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.postService.createPost(newPost)).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Failed to create post");
    }

    @Test
    void updatePost_shouldReturnUpdatedPost_whenValid() {
        // Given
        final var postToUpdate = MockDataUtil.createPostDto();
        postToUpdate.setId(1L);
        final var updatedPost = MockDataUtil.createPostDto();
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedPost);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.updatePost(postToUpdate.getId(), postToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        // When
        final var result = this.postService.updatePost(postToUpdate);

        // Then
        assertThat(result).isEqualTo(updatedPost);
        verify(this.postClient).updatePost(postToUpdate.getId(), postToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void updatePost_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var postToUpdate = MockDataUtil.createPostDto();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.postService.updatePost(postToUpdate));
    }

    @Test
    void updatePost_shouldThrowServiceException_whenUpdateFails() {
        // Given
        final var postToUpdate = MockDataUtil.createPostDto();
        postToUpdate.setId(1L);
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.updatePost(postToUpdate.getId(), postToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.postService.updatePost(postToUpdate)).isInstanceOf(ServiceException.class)
                .hasMessageContaining("Failed to update post");
    }

    @Test
    void deletePost_shouldSucceed_whenDeletionSuccessful() {
        // Given
        final var postId = 1L;
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.deletePost(postId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        this.postService.deletePost(postId);

        // Then
        verify(this.postClient).deletePost(postId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deletePost_shouldThrowServiceException_whenDeletionFails() {
        // Given
        final var postId = 1L;
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.deletePost(postId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.postService.deletePost(postId)).isInstanceOf(ServiceException.class);
    }

    @Test
    void getPostsByDateRange_shouldReturnPosts_whenValidDates() {
        // Given
        final LocalDate startDate = LocalDate.of(2023, 1, 1);
        final LocalDate endDate = LocalDate.of(2023, 12, 31);
        final var posts = Arrays.asList(MockDataUtil.createPostResponseDto(), MockDataUtil.createPostResponseDto());
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPostsByDateRange(startDate, endDate, TestUtil.TEST_AUTH_HEADER)).thenReturn(posts);

        // When
        final var result = this.postService.getPostsByDateRange(startDate, endDate);

        // Then
        assertThat(result).hasSize(2);
        verify(this.postClient).getPostsByDateRange(startDate, endDate, TestUtil.TEST_AUTH_HEADER);
    }
}
