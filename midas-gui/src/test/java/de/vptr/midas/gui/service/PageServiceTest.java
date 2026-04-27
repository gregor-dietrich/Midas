package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PageClient;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;
import jakarta.inject.Inject;

@ExtendWith(MockitoExtension.class)
class PageServiceTest {

    @RestClient
    @Inject
    @Mock
    PageClient pageClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PageService pageService;

    @Test
    void getAllPages_shouldReturnPages_whenAuthHeaderProvided() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final var expectedPages = List.of(new PageResponseDto(), new PageResponseDto());
        when(this.pageClient.getAllPages(0, 100, authHeader)).thenReturn(expectedPages);

        // When
        final var result = this.pageService.getAllPages(authHeader);

        // Then
        assertThat(result).isEqualTo(expectedPages);
        verify(this.pageClient).getAllPages(0, 100, authHeader);
    }

    @Test
    void getAllPages_shouldThrowServiceException_whenAuthHeaderIsNull() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.pageService.getAllPages(null));
    }

    @Test
    void getAllPages_shouldThrowServiceException_whenProcessingException() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        when(this.pageClient.getAllPages(0, 100, authHeader)).thenThrow(ExceptionTestUtil.createProcessingException());

        // When & Then
        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.pageService.getAllPages(authHeader));
    }

    @Test
    void getAllPages_shouldThrowAuthenticationException_when401() {
        // Given
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final var webAppException = ExceptionTestUtil.createWebApplicationException(401);
        when(this.pageClient.getAllPages(0, 100, authHeader)).thenThrow(webAppException);

        // When & Then
        ExceptionTestUtil.assertHandles401WithLogout(() -> this.pageService.getAllPages(authHeader), this.authService);
    }

    @Test
    void getPageById_shouldReturnPage_whenSuccessful() {
        // Given
        final var pageId = 1L;
        final var expectedPageResponse = MockDataUtil.createPageResponseDto(pageId, "Test Page");
        final var response = ResponseTestUtil.createOkResponse(expectedPageResponse);
        when(this.authService.isAuthenticated()).thenReturn(true);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.getPage(pageId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);

        // When
        final var result = this.pageService.getPageById(pageId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedPageResponse);
    }

    @Test
    void getPageById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // When & Then
        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.pageService.getPageById(1L));
    }

    @Test
    void createPage_shouldReturnCreatedPage_whenSuccessful() {
        // Given
        final var page = MockDataUtil.createPageDto();
        final var createdPage = MockDataUtil.createPageResponseDto(1L, "Created Page");
        createdPage.setContent("Created content");
        final var response = ResponseTestUtil.createCreatedResponse(createdPage);
        when(this.authService.isAuthenticated()).thenReturn(true);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.createPage(eq(page), eq(TestUtil.TEST_AUTH_HEADER))).thenReturn(response);

        // When
        final var result = this.pageService.createPage(page);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Created Page");
        assertThat(result.getContent()).isEqualTo("Created content");
    }

    @Test
    void createPage_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var page = MockDataUtil.createPageDto();
        when(this.authService.isAuthenticated()).thenReturn(false);

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.pageService.createPage(page));
    }

    @Test
    void updatePage_shouldReturnUpdatedPage_whenSuccessful() {
        // Given
        final var page = MockDataUtil.createPageDto();
        page.setId(1L);
        final var updatedPage = MockDataUtil.createPageResponseDto(1L, "Updated Page");
        updatedPage.setContent("Updated content");
        final var response = ResponseTestUtil.createOkResponse(updatedPage);
        when(this.authService.isAuthenticated()).thenReturn(true);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.updatePage(eq(page.getId()), eq(page), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(response);

        // When
        final var result = this.pageService.updatePage(page);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Updated Page");
        assertThat(result.getContent()).isEqualTo("Updated content");
    }

    @Test
    void updatePage_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var page = MockDataUtil.createPageDto();
        when(this.authService.isAuthenticated()).thenReturn(false);

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.pageService.updatePage(page));
    }

    @Test
    void deletePage_shouldReturnTrue_whenSuccessful() {
        // Given
        final var pageId = 1L;
        final var response = ResponseTestUtil.createMockResponse(204);
        when(this.authService.isAuthenticated()).thenReturn(true);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.deletePage(eq(pageId), eq(TestUtil.TEST_AUTH_HEADER))).thenReturn(response);

        // When
        final boolean result = this.pageService.deletePage(pageId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void deletePage_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        final var pageId = 1L;
        when(this.authService.isAuthenticated()).thenReturn(false);

        // When & Then
        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.pageService.deletePage(pageId));
    }

    @Test
    void searchPages_shouldReturnResults_whenAuthHeaderProvided() {
        final var authHeader = TestUtil.TEST_AUTH_HEADER;
        final var expectedPages = List.of(MockDataUtil.createPageResponseDto(1L, "Search Result"));
        when(this.pageClient.searchPages("result", authHeader)).thenReturn(expectedPages);

        final var result = this.pageService.searchPages("result", authHeader);

        assertThat(result).isEqualTo(expectedPages);
        verify(this.pageClient).searchPages("result", authHeader);
    }

    @Test
    void searchPages_shouldThrowAuthenticationException_whenAuthHeaderIsNull() {
        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.pageService.searchPages("result", null));
    }

    @Test
    void searchContent_shouldReturnResults_whenAuthenticated() {
        final var expectedPages = List.of(MockDataUtil.createPageResponseDto(1L, "Content Result"));
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.searchPages("content", TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPages);

        final var result = this.pageService.searchContent("content");

        assertThat(result).isEqualTo(expectedPages);
        verify(this.pageClient).searchPages("content", TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void searchContent_shouldThrowAuthenticationException_whenAuthHeaderUnavailable() {
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        ExceptionTestUtil.assertThrowsAuthenticationRequired(
                () -> this.pageService.searchContent("content"));
    }

    @Test
    void getPageById_shouldUseBackendPayload_whenUnexpectedStatusContainsBody() {
        final var pageId = 1L;
        final var response = ResponseTestUtil.createMockResponse(500, "{\"message\":\"Page lookup failed\"}");
        when(this.authService.isAuthenticated()).thenReturn(true);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.getPage(pageId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);

        ExceptionTestUtil.assertThrowsServiceException(() -> this.pageService.getPageById(pageId),
                "Page lookup failed");
    }
}
