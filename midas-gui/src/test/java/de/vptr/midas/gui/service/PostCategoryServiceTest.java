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
import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;

@SuppressWarnings("NullAway")
@ExtendWith(MockitoExtension.class)
class PostCategoryServiceTest {

    @Mock
    @RestClient
    PostCategoryClient categoryClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PostCategoryService postCategoryService;

    @Test
    void getAllCategories_shouldReturnCategoryList_whenAuthenticationProvided() {
        // Given
        final var categoryResponse1 = MockDataUtil.createPostCategoryResponseDto("1", "Category 1", null, 0, 0, true);
        final var categoryResponse2 = MockDataUtil.createPostCategoryResponseDto("2", "Category 2", null, 0, 0, true);
        final var expectedCategories = Arrays.asList(categoryResponse1, categoryResponse2);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getAllCategories(0, 100, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedCategories);

        // When
        final var result = this.postCategoryService.getAllCategories();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(categoryResponse1, categoryResponse2);
        verify(this.categoryClient).getAllCategories(0, 100, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    @DisplayName("Should throw authentication exception when auth header unavailable")
    void getAllCategories_shouldThrowAuthenticationException_whenAuthHeaderUnavailable() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.getAllCategories());
    }

    @Test
    void getAllCategories_shouldThrowServiceException_whenProcessingExceptionOccurs() {
        // Given
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getAllCategories(0, 100, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        // When & Then
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.postCategoryService.getAllCategories());
    }

    @Test
    void getAllCategories_shouldThrowAuthenticationException_when401Error() {
        // Given
        final var webException = ExceptionTestUtil.create401Exception();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getAllCategories(0, 100, TestUtil.TEST_AUTH_HEADER)).thenThrow(webException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.postCategoryService.getAllCategories(),
                this.authService);
    }

    @Test
    void getRootCategories_shouldReturnRootCategories_whenAuthenticated() {
        // Given
        final var rootCategoryResponse =
                MockDataUtil.createPostCategoryResponseDto("1", "Root Category", null, 0, 0, true);
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
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.getRootCategories());
    }

    @Test
    void getCategoryById_shouldReturnCategory_whenCategoryExists() {
        // Given
        final var categoryId = "1";
        final var expectedCategoryResponse =
                MockDataUtil.createPostCategoryResponseDto("1", "Test Category", null, 0, 0, false);
        final var mockResponse = ResponseTestUtil.createOkResponse(expectedCategoryResponse);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getCategory(categoryId, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postCategoryService.getCategoryById(categoryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getPublicId()).isEqualTo(categoryId);
        assertThat(result.get().getName()).isEqualTo("Test Category");
    }

    @Test
    void getCategoryById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.getCategoryById("1"));
    }

    @Test
    void getCategoryById_shouldReturnEmpty_whenCategoryNotFound() {
        // Given
        final var categoryId = "999";
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
        final var parentId = "1";
        final var childResponse1 = MockDataUtil.createPostCategoryResponseDto("2", "Child 1", "Parent", 0, 0, false);
        final var childResponse2 = MockDataUtil.createPostCategoryResponseDto("3", "Child 2", "Parent", 0, 0, false);
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
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.getCategoriesByParent("1"));
    }

    @Test
    void createCategory_shouldReturnCreatedCategory_whenValidCategory() {
        // Given
        final var newCategory = MockDataUtil.createPostCategoryDto();
        newCategory.setName("New Category");
        final var createdCategory = MockDataUtil.createPostCategoryResponseDto("1", "New Category", null, 0, 0, true);
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdCategory);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.createCategory(newCategory, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        // When
        final var result = this.postCategoryService.createCategory(newCategory);

        // Then
        assertThat(result.getPublicId()).isEqualTo("1");
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
        categoryToUpdate.setPublicId("1");
        categoryToUpdate.setName("Updated Category");
        final var updatedCategory =
                MockDataUtil.createPostCategoryResponseDto("1", "Updated Category", null, 0, 0, true);
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedCategory);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.updateCategory("1", categoryToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        // When
        final var result = this.postCategoryService.updateCategory(categoryToUpdate);

        // Then
        assertThat(result.getPublicId()).isEqualTo("1");
        assertThat(result.getName()).isEqualTo("Updated Category");
        verify(this.categoryClient).updateCategory("1", categoryToUpdate, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void deleteCategory_shouldReturnTrue_whenDeletionSuccessful() {
        // Given
        final var categoryId = "1";
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
        final var categoryId = "1";
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
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.getAllCategories(0, 100, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        ExceptionTestUtil.assertThrowsUnexpectedError(() -> this.postCategoryService.getAllCategories());
    }

    @Test
    void updateCategory_shouldThrowServiceException_whenUpdateFails() {
        final var categoryToUpdate = MockDataUtil.createPostCategoryDto();
        categoryToUpdate.setPublicId("1");
        categoryToUpdate.setName("Updated Category");
        final var mockResponse = ResponseTestUtil.createBadRequestResponse();

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.updateCategory("1", categoryToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        assertThatThrownBy(() -> this.postCategoryService.updateCategory(categoryToUpdate))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void searchCategories_shouldReturnResults_whenAuthenticated() {
        final String query = "test";
        final var category = MockDataUtil.createPostCategoryResponseDto("1", "Test Category", null, 0, 0, true);
        final var expected = Collections.singletonList(category);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.searchCategories(query, TestUtil.TEST_AUTH_HEADER)).thenReturn(expected);

        final var result = this.postCategoryService.searchCategories(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Category");
        verify(this.categoryClient).searchCategories(query, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void searchCategories_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.postCategoryService.searchCategories("test"));
    }

    @Test
    void createCategory_shouldResolveParentPublicId_fromParent_whenPublicIdIsNull() {
        // Simulate a category DTO where parent is set but parentPublicId was not carried over
        final var parentDto = new PostCategoryDto();
        parentDto.setPublicId("parent-1");

        final var newCategory = MockDataUtil.createPostCategoryDto();
        newCategory.setParent(parentDto); // sets parentPublicId = "parent-1" via setParent
        newCategory.setParentPublicId(null); // override to null to trigger the resolution branch

        final var createdCategory =
                MockDataUtil.createPostCategoryResponseDto("1", "New Category", "parent-1", 0, 0, false);
        final var mockResponse = ResponseTestUtil.createCreatedResponse(createdCategory);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.createCategory(newCategory, TestUtil.TEST_AUTH_HEADER)).thenReturn(mockResponse);

        this.postCategoryService.createCategory(newCategory);

        assertThat(newCategory.getParentPublicId()).isEqualTo("parent-1");
    }

    @Test
    void updateCategory_shouldResolveParentPublicId_fromParent_whenPublicIdIsNull() {
        final var parentDto = new PostCategoryDto();
        parentDto.setPublicId("parent-1");

        final var categoryToUpdate = MockDataUtil.createPostCategoryDto();
        categoryToUpdate.setPublicId("1");
        categoryToUpdate.setParent(parentDto); // sets parentPublicId = "parent-1"
        categoryToUpdate.setParentPublicId(null); // override to null to trigger the resolution branch

        final var updatedCategory =
                MockDataUtil.createPostCategoryResponseDto("1", "Updated Category", "parent-1", 0, 0, false);
        final var mockResponse = ResponseTestUtil.createOkResponse(updatedCategory);

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.categoryClient.updateCategory("1", categoryToUpdate, TestUtil.TEST_AUTH_HEADER))
                .thenReturn(mockResponse);

        this.postCategoryService.updateCategory(categoryToUpdate);

        assertThat(categoryToUpdate.getParentPublicId()).isEqualTo("parent-1");
    }
}
