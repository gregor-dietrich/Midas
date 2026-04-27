package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PostCategoryClient;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.inject.Inject;

@ExtendWith(MockitoExtension.class)
class PostCategoryServiceTest {

    @RestClient
    @Inject
    @Mock
    PostCategoryClient categoryClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PostCategoryService postCategoryService;

    @Test
    void getAllCategories_shouldReturnCategoryList_whenAuthenticationProvided() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final var categoryResponse1 = MockDataUtil.createPostCategoryResponseDto(1L, "Category 1", null, 0,
                0, true);
        final var categoryResponse2 = MockDataUtil.createPostCategoryResponseDto(2L, "Category 2", null, 0,
                0, true);
        final var expectedCategories = Arrays.asList(categoryResponse1, categoryResponse2);

        when(this.categoryClient.getAllCategories(0, 100, authHeader)).thenReturn(expectedCategories);

        // When
        final var result = this.postCategoryService.getAllCategories(authHeader);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(categoryResponse1, categoryResponse2);
        verify(this.categoryClient).getAllCategories(0, 100, authHeader);
    }

    @Test
    @DisplayName("Should throw authentication exception when auth header is null")
    void getAllCategories_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.getAllCategories(null));
    }

    @Test
    void getAllCategories_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        when(this.categoryClient.getAllCategories(0, 100, authHeader))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        // When & Then
        ExceptionTestUtil
                .assertThrowsBackendConnectionFailed(() -> this.postCategoryService.getAllCategories(authHeader));
    }

    @Test
    void getAllCategories_shouldThrowAuthenticationException_when401Error() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final var webException = ExceptionTestUtil.create401Exception();

        when(this.categoryClient.getAllCategories(0, 100, authHeader)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.postCategoryService.getAllCategories(authHeader),
                this.authService);
    }

    @Test
    void getRootCategories_shouldReturnRootCategories_whenAuthenticated() {
        // Given
        final var rootCategoryResponse = MockDataUtil.createPostCategoryResponseDto(1L, "Root Category",
                null, 0, 0, true);
        final var expectedCategories = Collections.singletonList(rootCategoryResponse);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getRootCategories(TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedCategories);

        // When
        final var result = this.postCategoryService.getRootCategories();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Root Category");
        verify(this.categoryClient).getRootCategories(TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getRootCategories_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.getRootCategories());
    }

    @Test
    void getCategoryById_shouldReturnCategory_whenCategoryExists() {
        // Given
        final var categoryId = 1L;
        final var expectedCategoryResponse = MockDataUtil.createPostCategoryResponseDto(categoryId,
                "Test Category", null, 0, 0,
                false);
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedCategoryResponse);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getCategory(categoryId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postCategoryService.getCategoryById(categoryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(categoryId);
        assertThat(result.get().getName()).isEqualTo("Test Category");
    }

    @Test
    void getCategoryById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.getCategoryById(1L));
    }

    @Test
    void getCategoryById_shouldReturnEmpty_whenCategoryNotFound() {
        // Given
        final var categoryId = 999L;
        final var mockResponse = ResponseTestUtil.createNotFoundResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getCategory(categoryId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postCategoryService.getCategoryById(categoryId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCategoriesByParent_shouldReturnChildCategories_whenParentExists() {
        // Given
        final var parentId = 1L;
        final var childResponse1 = MockDataUtil.createPostCategoryResponseDto(2L, "Child 1", "Parent", 0,
                0, false);
        final var childResponse2 = MockDataUtil.createPostCategoryResponseDto(3L, "Child 2", "Parent", 0,
                0, false);
        final var expectedChildren = Arrays.asList(childResponse1, childResponse2);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getCategoriesByParent(parentId, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(expectedChildren);

        // When
        final var result = this.postCategoryService.getCategoriesByParent(parentId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(childResponse1, childResponse2);
    }

    @Test
    void getCategoriesByParent_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.postCategoryService.getCategoriesByParent(1L));
    }

    @Test
    void createCategory_shouldReturnCreatedCategory_whenValidCategory() {
        // Given
        final var newCategory = MockDataUtil.createPostCategoryDto();
        newCategory.setName("New Category");
        final var createdCategory = MockDataUtil.createPostCategoryResponseDto(1L, "New Category", null, 0, 0, true);
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdCategory);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.createCategory(newCategory, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postCategoryService.createCategory(newCategory);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New Category");
        verify(this.categoryClient).createCategory(newCategory, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void createCategory_shouldThrowServiceException_whenCreationFails() {
        // Given
        final var newCategory = MockDataUtil.createPostCategoryDto();
        newCategory.setName("New Category");
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.createCategory(newCategory, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> this.postCategoryService.createCategory(newCategory))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void updateCategory_shouldReturnUpdatedCategory_whenValidCategory() {
        // Given
        final var categoryToUpdate = MockDataUtil.createPostCategoryDto();
        categoryToUpdate.setId(1L);
        categoryToUpdate.setName("Updated Category");
        final var updatedCategory = MockDataUtil.createPostCategoryResponseDto(1L, "Updated Category", null, 0, 0,
                true);
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedCategory);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.updateCategory(1L, categoryToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        // When
        final var result = this.postCategoryService.updateCategory(categoryToUpdate);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Updated Category");
        verify(this.categoryClient).updateCategory(1L, categoryToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteCategory_shouldReturnTrue_whenDeletionSuccessful() {
        // Given
        final var categoryId = 1L;
        final var mockResponse = ResponseTestUtil.createMockResponse(204);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.deleteCategory(categoryId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.postCategoryService.deleteCategory(categoryId);

        // Then
        assertThat(result).isTrue();
        verify(this.categoryClient).deleteCategory(categoryId, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteCategory_shouldReturnFalse_whenDeletionFails() {
        // Given
        final var categoryId = 1L;
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.deleteCategory(categoryId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final boolean result = this.postCategoryService.deleteCategory(categoryId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getAllCategories_shouldThrowServiceException_whenUnexpectedExceptionOccurs() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        when(this.categoryClient.getAllCategories(0, 100, authHeader))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.postCategoryService.getAllCategories(authHeader));
    }
}
