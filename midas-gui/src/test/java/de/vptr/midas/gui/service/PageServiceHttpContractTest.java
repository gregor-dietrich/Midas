package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Pins {@link PageService} behaviour against real HTTP responses from a stub backend.
 *
 * <p>
 * These assertions describe what the service does with a given HTTP status and payload, not how it calls its REST
 * client, so they remain meaningful when the client interfaces are regenerated and the Authorization header moves out
 * of the method signature.
 */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class PageServiceHttpContractTest {

    private static final String AUTH_HEADER = "Basic dGVzdDp0ZXN0";

    private static final String PAGE_JSON = """
            {"publicId":"01AN4Z07BY79KA1307SR9X4MV4","title":"Impressum","content":"Body text"}""";

    @Inject
    PageService pageService;

    @InjectMock
    AuthService authService;

    @BeforeEach
    void setUp() {
        StubBackend.reset();
        when(this.authService.requireAuthorizationHeader()).thenReturn(AUTH_HEADER);
        when(this.authService.getAuthorizationHeader()).thenReturn(AUTH_HEADER);
    }

    /** The invariant that must survive the Authorization header moving out of the client method signatures. */
    @AfterEach
    void everyRequestSendsCredentials() {
        assertThat(StubBackend.requests()).isNotEmpty()
                .allSatisfy(request -> assertThat(request.authorization()).isEqualTo(AUTH_HEADER));
    }

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

    /**
     * Pins current behaviour, which is not what the mock-based unit tests suggest: the REST client turns any 4xx into a
     * WebApplicationException before the service sees a Response, so the service's own not-found branch never runs and
     * the caller gets the generic failure instead of an empty Optional.
     */
    @Test
    void getPageById_throwsGenericFailure_whenNotFound() {
        StubBackend.stub("GET", "/pages/missing", 404, """
                {"status":404,"error":"Not Found","message":"The requested resource was not found."}""");

        assertThatThrownBy(() -> this.pageService.getPageById("missing")).isInstanceOf(ServiceException.class)
                .hasMessage("Backend request failed");
    }

    @Test
    void createPage_returnsCreatedPage_on201() {
        StubBackend.stub("POST", "/pages", 201, PAGE_JSON);

        final var created = this.pageService.createPage(new PageDto("", "Impressum", "Body text"));

        assertThat(created.getTitle()).isEqualTo("Impressum");
        assertThat(StubBackend.onlyRequest().body()).contains("Impressum");
    }

    @Test
    void deletePage_succeeds_on204() {
        StubBackend.stub("DELETE", "/pages/01AN4Z07BY79KA1307SR9X4MV4", 204, null);

        this.pageService.deletePage("01AN4Z07BY79KA1307SR9X4MV4");

        assertThat(StubBackend.onlyRequest().method()).isEqualTo("DELETE");
    }

    /**
     * As above: the service's "Page not found" branch is unreachable because the client throws first.
     */
    @Test
    void deletePage_throwsGenericFailure_on404() {
        StubBackend.stub("DELETE", "/pages/missing", 404, null);

        assertThatThrownBy(() -> this.pageService.deletePage("missing")).isInstanceOf(ServiceException.class)
                .hasMessage("Backend request failed");
    }

    @Test
    void anyCall_logsOutAndThrows_on401() {
        StubBackend.stub("GET", "/pages", 401, null);

        assertThatThrownBy(() -> this.pageService.getAllPages()).isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
    }
}
