package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Pins {@link PostCategoryService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class PostCategoryServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String CATEGORY_ID = "01JD6E8G0H2J4K6M8N0P2Q4R6S";
    private static final String PARENT_ID = "01JD6E8G0H2J4K6M8N0P2Q4R7T";
    private static final String CATEGORY_PATH = "/categories/" + CATEGORY_ID;

    private static final String CATEGORY_JSON = """
            {"publicId":"01JD6E8G0H2J4K6M8N0P2Q4R6S","name":"Hardware","parentPublicId":"01JD6E8G0H2J4K6M8N0P2Q4R7T",\
            "parentName":"Topics","isRootCategory":false,"childrenCount":0,"postsCount":4,"childrenPublicIds":[],\
            "created":"2026-03-01T09:00:00","lastEdit":"2026-03-02T09:00:00"}""";

    private static final String CATEGORY_VALIDATION_JSON = """
            {"status":422,"error":"Unprocessable Entity","message":"name must not be blank"}""";

    @Inject
    PostCategoryService categoryService;

    /** A category whose parent is set as an object only, so the service has to derive {@code parentPublicId}. */
    private static PostCategoryDto withParentObjectOnly() {
        final var parent = new PostCategoryDto();
        parent.setPublicId(PARENT_ID);
        parent.setName("Topics");
        final var category = new PostCategoryDto();
        category.setPublicId(CATEGORY_ID);
        category.setName("Hardware");
        category.setParent(parent);
        category.setParentPublicId(null);
        return category;
    }

    private static Arguments categoryLookup(final String suffix, final List<String> query,
            final Function<PostCategoryService, List<?>> call) {
        return Arguments.of(suffix, query, call);
    }

    static Stream<Arguments> categoryLookups() {
        return Stream.of(categoryLookup("/categories/root", List.of(), s -> s.getRootCategories()),
                categoryLookup("/categories/parent/" + PARENT_ID, List.of(), s -> s.getCategoriesByParent(PARENT_ID)),
                categoryLookup("/categories/search", List.of("query=hard"), s -> s.searchCategories("hard")),
                categoryLookup("/categories", List.of("page=0", "size=100"), s -> s.getAllCategories()));
    }

    /** {@code getAllCategories} always asks for the first page at the maximum page size. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("categoryLookups")
    void categoryLookups_mapPayloadAndSendQuery(final String suffix, final List<String> query,
            final Function<PostCategoryService, List<?>> call) {
        StubBackend.stub("GET", suffix, 200, "[" + CATEGORY_JSON + "]");

        assertThat(call.apply(this.categoryService)).singleElement().hasFieldOrPropertyWithValue("name", "Hardware")
                .hasFieldOrPropertyWithValue("parentName", "Topics");

        final var received = StubBackend.onlyRequest();
        assertThat(received.path()).endsWith(suffix);
        assertThat(query).allSatisfy(param -> assertThat(received.query()).contains(param));
    }

    @Test
    void getCategoryById_returnsValue_whenFound() {
        StubBackend.stub("GET", CATEGORY_PATH, 200, CATEGORY_JSON);

        final var found = this.categoryService.getCategoryById(CATEGORY_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getParentPublicId()).isEqualTo(PARENT_ID);
        assertThat(found.get().getPostsCount()).isEqualTo(4);
        assertThat(StubBackend.onlyRequest().path()).endsWith(CATEGORY_PATH);
    }

    /** A 404 means the category does not exist. */
    @Test
    void getCategoryById_returnsEmpty_whenNotFound() {
        StubBackend.stub("GET", CATEGORY_PATH, 404, null);

        assertThat(this.categoryService.getCategoryById(CATEGORY_ID)).isEmpty();
    }

    @Test
    void createCategory_derivesParentId_andMapsCreated_on201() {
        StubBackend.stub("POST", "/categories", 201, CATEGORY_JSON);

        final var created = this.categoryService.createCategory(withParentObjectOnly());

        assertThat(created.getName()).isEqualTo("Hardware");
        assertThat(created.getParentPublicId()).isEqualTo(PARENT_ID);
        assertThat(created.getParent()).isNotNull().extracting(PostCategoryDto::getName).isEqualTo("Topics");
        final var received = StubBackend.onlyRequest();
        assertThat(received.method()).isEqualTo("POST");
        assertThat(received.body()).contains("\"name\":\"Hardware\"", "\"parentPublicId\":\"" + PARENT_ID + "\"");
    }

    @Test
    void updateCategory_derivesParentId_andMapsUpdated_on200() {
        StubBackend.stub("PUT", CATEGORY_PATH, 200, CATEGORY_JSON);

        final var updated = this.categoryService.updateCategory(withParentObjectOnly());

        assertThat(updated.getPublicId()).isEqualTo(CATEGORY_ID);
        final var received = StubBackend.onlyRequest();
        assertThat(received.path()).endsWith(CATEGORY_PATH);
        assertThat(received.body()).contains("\"parentPublicId\":\"" + PARENT_ID + "\"");
    }

    @Test
    void deleteCategory_succeeds_on204() {
        StubBackend.stub("DELETE", CATEGORY_PATH, 204, null);

        assertThatCode(() -> this.categoryService.deleteCategory(CATEGORY_ID)).doesNotThrowAnyException();

        assertThat(StubBackend.onlyRequest().method()).isEqualTo("DELETE");
    }

    /** deleteCategory carries no not-found message, so a 404 surfaces as the generic failure. */
    @Test
    void deleteCategory_throwsGenericFailure_on404() {
        this.assertCategoryFailure("DELETE", CATEGORY_PATH, 404, null, s -> s.deleteCategory(CATEGORY_ID),
                "Backend request failed");
    }

    private static Arguments rejectedCategoryWrite(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostCategoryService> call, final String message) {
        return Arguments.of(method, suffix, status, body, call, message);
    }

    static Stream<Arguments> rejectedCategoryWrites() {
        return Stream.of(rejectedCategoryWrite("POST", "/categories", 422, CATEGORY_VALIDATION_JSON,
                s -> s.createCategory(withParentObjectOnly()), "Failed to create category: name must not be blank"),
                rejectedCategoryWrite("PUT", CATEGORY_PATH, 422, CATEGORY_VALIDATION_JSON,
                        s -> s.updateCategory(withParentObjectOnly()),
                        "Failed to update category: name must not be blank"));
    }

    /** 422 validation errors surface the operation's failure prefix. */
    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("rejectedCategoryWrites")
    void categoryWrites_failWithPrefixedMessage(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostCategoryService> call, final String message) {
        this.assertCategoryFailure(method, suffix, status, body, call, message);
    }

    @Test
    void anyCall_logsOutAndThrows_on401() {
        StubBackend.stub("DELETE", CATEGORY_PATH, 401, null);

        assertThatThrownBy(() -> this.categoryService.deleteCategory(CATEGORY_ID))
                .isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
    }

    private void assertCategoryFailure(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostCategoryService> call, final String message) {
        StubBackend.stub(method, suffix, status, body);

        assertThatThrownBy(() -> call.accept(this.categoryService)).isInstanceOf(ServiceException.class)
                .hasMessage(message);
    }
}
