package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Pins {@link PostService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class PostServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String POST_ID = "01JB8Y2K4M6N8P0Q2R4S6T8V0W";
    private static final String USER_ID = "01JB8Y2K4M6N8P0Q2R4S6T8V1X";
    private static final String CATEGORY_ID = "01JB8Y2K4M6N8P0Q2R4S6T8V2Y";
    private static final String POST_PATH = "/posts/" + POST_ID;
    private static final LocalDate JANUARY_FIRST = LocalDate.of(2026, 1, 1);
    private static final LocalDate JANUARY_LAST = LocalDate.of(2026, 1, 31);

    private static final String POST_JSON = """
            {"publicId":"01JB8Y2K4M6N8P0Q2R4S6T8V0W","title":"Release notes","content":"What changed",\
            "userPublicId":"01JB8Y2K4M6N8P0Q2R4S6T8V1X","username":"author",\
            "categoryPublicId":"01JB8Y2K4M6N8P0Q2R4S6T8V2Y","categoryName":"News","published":true,\
            "commentable":false,"created":"2026-01-15T10:30:00","lastEdit":"2026-01-16T08:00:00","commentsCount":3}""";

    private static final String POST_NOT_FOUND_JSON = """
            {"status":404,"error":"Not Found","message":"Post not found"}""";

    private static final String POST_VALIDATION_JSON = """
            {"status":422,"error":"Unprocessable Entity","message":"title must not be blank"}""";

    @Inject
    PostService postService;

    private static PostDto draft() {
        final var post = new PostDto();
        post.setTitle("Release notes");
        post.setContent("What changed");
        post.setCategoryPublicId(CATEGORY_ID);
        post.setPublished(true);
        return post;
    }

    private static PostDto existing() {
        final var post = draft();
        post.setPublicId(POST_ID);
        return post;
    }

    private static Arguments lookup(final String suffix, final List<String> query,
            final Function<PostService, List<?>> call) {
        return Arguments.of(suffix, query, call);
    }

    static Stream<Arguments> lookups() {
        return Stream.of(lookup("/posts", List.of("page=0", "size=20"), s -> s.getAllPosts()),
                lookup("/posts/published", List.of("page=0", "size=20"), s -> s.getPublishedPosts()),
                lookup("/posts/published", List.of("page=3", "size=20"), s -> s.getPublishedPosts(3, 0)),
                lookup("/posts/published",
                        List.of("categoryPublicId=" + CATEGORY_ID, "startDate=2026-01-01", "endDate=2026-01-31",
                                "query=release", "page=2", "size=10"),
                        s -> s.getPublishedPosts(CATEGORY_ID, JANUARY_FIRST, JANUARY_LAST, "release", 2, 10)),
                lookup("/posts/user/" + USER_ID, List.of(), s -> s.getPostsByUser(USER_ID)),
                lookup("/posts/category/" + CATEGORY_ID, List.of(), s -> s.getPostsByCategory(CATEGORY_ID)),
                lookup("/posts/search", List.of("query=release"), s -> s.searchPosts("release")),
                lookup("/posts/date-range", List.of("startDate=2026-01-01", "endDate=2026-01-31"),
                        s -> s.getPostsByDateRange(JANUARY_FIRST, JANUARY_LAST)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("lookups")
    void listLookups_mapPayloadAndSendComputedQuery(final String suffix, final List<String> query,
            final Function<PostService, List<?>> call) {
        StubBackend.stub("GET", suffix, 200, "[" + POST_JSON + "]");

        assertThat(call.apply(this.postService)).extracting("title", "username")
                .containsExactly(tuple("Release notes", "author"));

        final var request = StubBackend.onlyRequest();
        assertThat(request.path()).endsWith(suffix);
        assertThat(query).allSatisfy(param -> assertThat(request.query()).contains(param));
    }

    @ParameterizedTest(name = "page={0} size={1}")
    @CsvSource({ ",,0,20", "-3,500,0,100", "4,0,4,20" })
    void getAllPosts_normalisesPagination(@Nullable final Integer page, @Nullable final Integer size,
            final int expectedPage, final int expectedSize) {
        StubBackend.stub("GET", "/posts", 200, "[]");

        assertThat(this.postService.getAllPosts(page, size)).isEmpty();

        assertThat(StubBackend.onlyRequest().query()).contains("page=" + expectedPage, "size=" + expectedSize);
    }

    @Test
    void getPostById_returnsValue_whenFound() {
        StubBackend.stub("GET", POST_PATH, 200, POST_JSON);

        final var found = this.postService.getPostById(POST_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getCategoryName()).isEqualTo("News");
        assertThat(found.get().getCommentsCount()).isEqualTo(3L);
        assertThat(StubBackend.onlyRequest().path()).endsWith(POST_PATH);
    }

    /** A 404 means the post does not exist. */
    @Test
    void getPostById_returnsEmpty_whenNotFound() {
        StubBackend.stub("GET", POST_PATH, 404, POST_NOT_FOUND_JSON);

        assertThat(this.postService.getPostById(POST_ID)).isEmpty();
    }

    @Test
    void createPost_returnsCreatedPost_on201() {
        StubBackend.stub("POST", "/posts", 201, POST_JSON);

        final var created = this.postService.createPost(draft());

        assertThat(created.getPublicId()).isEqualTo(POST_ID);
        assertThat(created.getUsername()).isEqualTo("author");
        final var request = StubBackend.onlyRequest();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.body()).contains("\"title\":\"Release notes\"", "\"published\":true",
                "\"categoryPublicId\":\"" + CATEGORY_ID + "\"");
    }

    @Test
    void updatePost_returnsUpdatedPost_on200() {
        StubBackend.stub("PUT", POST_PATH, 200, POST_JSON);

        final var updated = this.postService.updatePost(existing());

        assertThat(updated.getTitle()).isEqualTo("Release notes");
        final var request = StubBackend.onlyRequest();
        assertThat(request.path()).endsWith(POST_PATH);
        assertThat(request.body()).contains("\"content\":\"What changed\"");
    }

    @Test
    void deletePost_succeeds_on204() {
        StubBackend.stub("DELETE", POST_PATH, 204, null);

        this.postService.deletePost(POST_ID);

        assertThat(StubBackend.onlyRequest().path()).endsWith(POST_PATH);
    }

    /** A 404 on delete surfaces the service's not-found message. */
    @Test
    void deletePost_throwsNotFound_on404() {
        this.assertFailure("DELETE", POST_PATH, 404, null, s -> s.deletePost(POST_ID), "Post not found");
    }

    private static Arguments rejection(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostService> call, final String message) {
        return Arguments.of(method, suffix, status, body, call, message);
    }

    static Stream<Arguments> rejectedWrites() {
        return Stream.of(
                rejection("POST", "/posts", 422, POST_VALIDATION_JSON, s -> s.createPost(draft()),
                        "Failed to create post: title must not be blank"),
                rejection("PUT", POST_PATH, 422, POST_VALIDATION_JSON, s -> s.updatePost(existing()),
                        "Failed to update post: title must not be blank"));
    }

    /** 422 validation errors surface the operation's failure prefix. */
    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("rejectedWrites")
    void writes_failWithPrefixedMessage_onValidationError(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostService> call, final String message) {
        this.assertFailure(method, suffix, status, body, call, message);
    }

    @Test
    void anyCall_logsOutAndThrows_on401() {
        StubBackend.stub("GET", "/posts/published", 401, null);

        assertThatThrownBy(() -> this.postService.getPublishedPosts()).isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
    }

    private void assertFailure(final String method, final String suffix, final int status, @Nullable final String body,
            final Consumer<PostService> call, final String message) {
        StubBackend.stub(method, suffix, status, body);

        assertThatThrownBy(() -> call.accept(this.postService)).isInstanceOf(ServiceException.class)
                .hasMessage(message);
    }
}
