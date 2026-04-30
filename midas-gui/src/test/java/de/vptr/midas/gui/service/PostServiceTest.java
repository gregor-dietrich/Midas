package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PostClient;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @RestClient
    @Inject
    @Mock
    PostClient postClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PostService postService;

    @Test
    void getAllPosts_shouldReturnPosts_whenAuthHeaderProvided() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final var expectedPosts = Arrays.asList(
                MockDataUtil.createPostResponseDto(),
                MockDataUtil.createPostResponseDto());
        when(this.postClient.getAllPosts(authHeader)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getAllPosts(authHeader);

        // Then
        assertThat(result).isEqualTo(expectedPosts);
        verify(this.postClient).getAllPosts(authHeader);
    }

    @Test
    void getAllPosts_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getAllPosts(null));
    }

    @Test
    void getAllPosts_shouldThrowServiceException_whenProcessingException() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        when(this.postClient.getAllPosts(authHeader)).thenThrow(new ProcessingException("Connection failed"));

        // When & Then
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.postService.getAllPosts(authHeader));
    }

    @Test
    void getAllPosts_shouldThrowAuthenticationException_when401() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final WebApplicationException webAppException = new WebApplicationException(401);
        when(this.postClient.getAllPosts(authHeader)).thenThrow(webAppException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.postService.getAllPosts(authHeader), this.authService);
    }

    @Test
    void getPublishedPosts_shouldReturnPosts_whenAuthenticated() {
        // Given
        final var expectedPosts = Arrays.asList(
                MockDataUtil.createPostResponseDto(),
                MockDataUtil.createPostResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPublishedPosts(TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getPublishedPosts(this.authService.getAuthorizationHeader());

        // Then
        assertThat(result).isEqualTo(expectedPosts);
        verify(this.postClient).getPublishedPosts(TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getPublishedPosts_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getPublishedPosts(null));
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
        final var expectedPosts = Arrays.asList(
                MockDataUtil.createPostResponseDto(),
                MockDataUtil.createPostResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPostsByUser(userId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getPostsByUser(userId);

        // Then
        assertThat(result).isEqualTo(expectedPosts);
    }

    @Test
    void getPostsByUser_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postService.getPostsByUser(1L));
    }

    @Test
    void getPostsByCategory_shouldReturnPosts_whenAuthenticated() {
        // Given
        final var categoryId = 1L;
        final var expectedPosts = Arrays.asList(
                MockDataUtil.createPostResponseDto(),
                MockDataUtil.createPostResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.getPostsByCategory(categoryId, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPosts);

        // When
        final var result = this.postService.getPostsByCategory(categoryId);

        // Then
        assertThat(result).isEqualTo(expectedPosts);
    }

    @Test
    void getPostsByCategory_shouldThrowAuthenticationException_whenNotAuthenticated() {
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
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

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
        assertThatThrownBy(() -> this.postService.createPost(newPost))
                .isInstanceOf(ServiceException.class)
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
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

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
        assertThatThrownBy(() -> this.postService.updatePost(postToUpdate))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Failed to update post");
    }

    @Test
    void deletePost_shouldReturnTrue_whenDeletionSuccessful() {
        // Given
        final var postId = 1L;
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.deletePost(postId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.postService.deletePost(postId);

        // Then
        assertThat(result).isTrue();
        verify(this.postClient).deletePost(postId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deletePost_shouldReturnFalse_whenDeletionFails() {
        // Given
        final var postId = 1L;
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.postClient.deletePost(postId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.postService.deletePost(postId);

        // Then
        assertThat(result).isFalse();
    }
}
