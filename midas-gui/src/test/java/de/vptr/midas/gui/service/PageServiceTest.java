package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PageClient;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.ExceptionTestUtil;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.ResponseTestUtil;
import de.vptr.midas.gui.util.ServiceTestUtil;
import de.vptr.midas.gui.util.TestUtil;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LinguisticNaming")
class PageServiceTest {

    @Mock
    PageClient pageClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PageService pageService;

    @Test
    void getAllPages_shouldReturnPages_whenAuthenticated() {
        final var expectedPages = List.of(new PageResponseDto(), new PageResponseDto());

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.getAllPages(0, 100, TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPages);

        final var result = this.pageService.getAllPages();

        assertThat(result).isEqualTo(expectedPages);
        verify(this.pageClient).getAllPages(0, 100, TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void getAllPages_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.pageService.getAllPages());
    }

    @Test
    void getAllPages_shouldThrowServiceException_whenProcessingException() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.getAllPages(0, 100, TestUtil.TEST_AUTH_HEADER))
                .thenThrow(ExceptionTestUtil.createProcessingException());

        ExceptionTestUtil.assertThrowsBackendConnectionFailed(() -> this.pageService.getAllPages());
    }

    @Test
    void getAllPages_shouldThrowAuthenticationException_when401() {
        ServiceTestUtil.setupAuthHeader(this.authService);
        final var webAppException = ExceptionTestUtil.createWebApplicationException(401);
        when(this.pageClient.getAllPages(0, 100, TestUtil.TEST_AUTH_HEADER)).thenThrow(webAppException);

        ExceptionTestUtil.assertHandles401WithLogout(() -> this.pageService.getAllPages(), this.authService);
    }

    @Test
    void getPageById_shouldReturnPage_whenSuccessful() {
        final var pageId = 1L;
        final var expectedPageResponse = MockDataUtil.createPageResponseDto(pageId, "Test Page");
        final var response = ResponseTestUtil.createOkResponse(expectedPageResponse);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.getPage(pageId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);

        final var result = this.pageService.getPageById(pageId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedPageResponse);
    }

    @Test
    void getPageById_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.pageService.getPageById(1L));
    }

    @Test
    void createPage_shouldReturnCreatedPage_whenSuccessful() {
        final var page = MockDataUtil.createPageDto();
        final var createdPage = MockDataUtil.createPageResponseDto(1L, "Created Page");
        createdPage.setContent("Created content");
        final var response = ResponseTestUtil.createCreatedResponse(createdPage);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.createPage(eq(page), eq(TestUtil.TEST_AUTH_HEADER))).thenReturn(response);

        final var result = this.pageService.createPage(page);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Created Page");
        assertThat(result.getContent()).isEqualTo("Created content");
    }

    @Test
    void createPage_shouldThrowAuthenticationException_whenNotAuthenticated() {
        final var page = MockDataUtil.createPageDto();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.pageService.createPage(page));
    }

    @Test
    void updatePage_shouldReturnUpdatedPage_whenSuccessful() {
        final var page = MockDataUtil.createPageDto();
        page.setId(1L);
        final var updatedPage = MockDataUtil.createPageResponseDto(1L, "Updated Page");
        updatedPage.setContent("Updated content");
        final var response = ResponseTestUtil.createOkResponse(updatedPage);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.updatePage(eq(page.getId()), eq(page), eq(TestUtil.TEST_AUTH_HEADER)))
                .thenReturn(response);

        final var result = this.pageService.updatePage(page);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Updated Page");
        assertThat(result.getContent()).isEqualTo("Updated content");
    }

    @Test
    void updatePage_shouldThrowAuthenticationException_whenNotAuthenticated() {
        final var page = MockDataUtil.createPageDto();
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.pageService.updatePage(page));
    }

    @Test
    void deletePage_shouldSucceed_whenSuccessful() {
        final var pageId = 1L;
        final var response = ResponseTestUtil.createMockResponse(204);
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.deletePage(eq(pageId), eq(TestUtil.TEST_AUTH_HEADER))).thenReturn(response);

        this.pageService.deletePage(pageId);

        verify(this.pageClient).deletePage(eq(pageId), eq(TestUtil.TEST_AUTH_HEADER));
    }

    @Test
    void deletePage_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsUserNotAuthenticated(() -> this.pageService.deletePage(1L));
    }

    @Test
    void searchPages_shouldReturnResults_whenAuthenticated() {
        final var expectedPages = List.of(MockDataUtil.createPageResponseDto(1L, "Search Result"));

        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.searchPages("result", TestUtil.TEST_AUTH_HEADER)).thenReturn(expectedPages);

        final var result = this.pageService.searchPages("result");

        assertThat(result).isEqualTo(expectedPages);
        verify(this.pageClient).searchPages("result", TestUtil.TEST_AUTH_HEADER);
    }

    @Test
    void searchPages_shouldThrowAuthenticationException_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.pageService.searchPages("result"));
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
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));

        ExceptionTestUtil.assertThrowsAuthenticationRequired(() -> this.pageService.searchContent("content"));
    }

    @Test
    void getPageById_shouldUseBackendPayload_whenUnexpectedStatusContainsBody() {
        final var pageId = 1L;
        final var response = ResponseTestUtil.createMockResponse(500, "{\"message\":\"Page lookup failed\"}");
        ServiceTestUtil.setupAuthHeader(this.authService);
        when(this.pageClient.getPage(pageId, TestUtil.TEST_AUTH_HEADER)).thenReturn(response);

        ExceptionTestUtil.assertThrowsServiceException(() -> this.pageService.getPageById(pageId),
                "Page lookup failed");
    }
}
