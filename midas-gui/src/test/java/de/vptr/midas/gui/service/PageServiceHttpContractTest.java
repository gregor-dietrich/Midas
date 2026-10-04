package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Pins {@link PageService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class PageServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String PAGE_JSON = """
            {"publicId":"01AN4Z07BY79KA1307SR9X4MV4","title":"Impressum","content":"Body text"}""";

    @Inject
    PageService pageService;

    @Test
    void getAllPages_mapsPayload() {
        StubBackend.stub("GET", "/pages", 200, "[" + PAGE_JSON + "]");

        final var pages = this.pageService.getAllPages();

        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).getTitle()).isEqualTo("Impressum");
    }

    @Test
    void getAllPages_normalisesPaginationToDefaults() {
        StubBackend.stub("GET", "/pages", 200, "[]");

        this.pageService.getAllPages();

        assertThat(StubBackend.onlyRequest().query()).contains("page=0").contains("size=100");
    }

    @Test
    void getPageById_returnsValue_whenFound() {
        StubBackend.stub("GET", "/pages/01AN4Z07BY79KA1307SR9X4MV4", 200, PAGE_JSON);

        final var found = this.pageService.getPageById("01AN4Z07BY79KA1307SR9X4MV4");

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("Body text");
    }

    /** A 404 means the page does not exist. */
    @Test
    void getPageById_returnsEmpty_whenNotFound() {
        StubBackend.stub("GET", "/pages/missing", 404, """
                {"status":404,"error":"Not Found","message":"The requested resource was not found."}""");

        assertThat(this.pageService.getPageById("missing")).isEmpty();
    }

    @Test
    void createPage_returnsCreatedPage_on201() {
        StubBackend.stub("POST", "/pages", 201, PAGE_JSON);

        final var created = this.pageService.createPage(new PageDto("", "Impressum", "Body text"));

        assertThat(created.getTitle()).isEqualTo("Impressum");
        assertThat(StubBackend.onlyRequest().body()).contains("Impressum");
    }

    @ParameterizedTest(name = "page={0} size={1}")
    @CsvSource({ "-2,0,0,20", "3,500,3,100" })
    void getAllPagesPaginated_normalisesPagination(final int page, final int size, final int sentPage,
            final int sentSize) {
        StubBackend.stub("GET", "/pages", 200, "[" + PAGE_JSON + "]");

        assertThat(this.pageService.getAllPages(page, size)).extracting(PageResponseDto::getTitle)
                .containsExactly("Impressum");
        assertThat(StubBackend.onlyRequest().query()).contains("page=" + sentPage, "size=" + sentSize);
    }

    @Test
    void updatePage_putsToItsId_andReturnsUpdatedPage() {
        StubBackend.stub("PUT", "/pages/01AN4Z07BY79KA1307SR9X4MV4", 200, PAGE_JSON);

        final var updated =
                this.pageService.updatePage(new PageDto("01AN4Z07BY79KA1307SR9X4MV4", "Impressum", "Body text"));

        assertThat(updated.getContent()).isEqualTo("Body text");
        final var request = StubBackend.onlyRequest();
        assertThat(request.method()).isEqualTo("PUT");
        assertThat(request.body()).contains("\"title\":\"Impressum\"");
    }

    @Test
    void updatePage_reportsBackendMessageWithPrefix_on422() {
        StubBackend.stub("PUT", "/pages/01AN4Z07BY79KA1307SR9X4MV4", 422, """
                {"status":422,"error":"Unprocessable Entity","message":"title must not be blank"}""");

        assertThatThrownBy(() -> this.pageService.updatePage(new PageDto("01AN4Z07BY79KA1307SR9X4MV4", "", "")))
                .isInstanceOf(ServiceException.class).hasMessage("Failed to update page: title must not be blank");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(booleans = { true, false })
    void searches_sendQueryAndMapResults(final boolean contentSearch) {
        StubBackend.stub("GET", "/pages/search", 200, "[" + PAGE_JSON + "]");

        final var found = contentSearch ? this.pageService.searchContent("impr") : this.pageService.searchPages("impr");

        assertThat(found).extracting(PageResponseDto::getPublicId).containsExactly("01AN4Z07BY79KA1307SR9X4MV4");
        assertThat(StubBackend.onlyRequest().query()).isEqualTo("query=impr");
    }

    @Test
    void deletePage_succeeds_on204() {
        StubBackend.stub("DELETE", "/pages/01AN4Z07BY79KA1307SR9X4MV4", 204, null);

        this.pageService.deletePage("01AN4Z07BY79KA1307SR9X4MV4");

        assertThat(StubBackend.onlyRequest().method()).isEqualTo("DELETE");
    }

    /** A 404 on delete surfaces the service's not-found message. */
    @Test
    void deletePage_throwsNotFound_on404() {
        StubBackend.stub("DELETE", "/pages/missing", 404, null);

        assertThatThrownBy(() -> this.pageService.deletePage("missing")).isInstanceOf(ServiceException.class)
                .hasMessage("Page not found");
    }

    /** Credentials come from MidasClientHeadersFactory, which refuses before anything is sent. */
    @Test
    void anyCall_throwsWithoutSendingRequest_whenNotAuthenticated() {
        when(this.authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("User not authenticated"));

        assertThatThrownBy(() -> this.pageService.getAllPages()).isInstanceOf(AuthenticationException.class)
                .hasMessage("User not authenticated");

        assertThat(StubBackend.requests()).isEmpty();
    }

    @Test
    void anyCall_logsOutAndThrows_on401() {
        StubBackend.stub("GET", "/pages", 401, null);

        assertThatThrownBy(() -> this.pageService.getAllPages()).isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
    }
}
