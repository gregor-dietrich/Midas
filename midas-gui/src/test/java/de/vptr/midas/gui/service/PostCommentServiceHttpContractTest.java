package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Pins {@link PostCommentService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class PostCommentServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String COMMENT_ID = "01JC3D5F7G9H1J3K5M7N9P1Q3R";
    private static final String COMMENTED_POST_ID = "01JC3D5F7G9H1J3K5M7N9P1Q4S";
    private static final String COMMENTER_ID = "01JC3D5F7G9H1J3K5M7N9P1Q5T";
    private static final String COMMENT_PATH = "/comments/" + COMMENT_ID;

    private static final String COMMENT_JSON = """
            {"publicId":"01JC3D5F7G9H1J3K5M7N9P1Q3R","content":"Thanks for the update",\
            "postPublicId":"01JC3D5F7G9H1J3K5M7N9P1Q4S","postTitle":"Release notes",\
            "userPublicId":"01JC3D5F7G9H1J3K5M7N9P1Q5T","username":"reader",\
            "created":"2026-02-01T12:00:00","lastEdit":"2026-02-01T12:05:00"}""";

    private static final String COMMENT_VALIDATION_JSON = """
            {"status":422,"error":"Unprocessable Entity","message":"content must not be blank"}""";

    @Inject
    PostCommentService commentService;

    /** A comment as a view hands it over, carrying read-only fields the service must not forward. */
    private static PostCommentDto fromView() {
        final var comment = new PostCommentDto();
        comment.setPublicId(COMMENT_ID);
        comment.setContent("Thanks for the update");
        comment.setPostPublicId(COMMENTED_POST_ID);
        comment.setUsername("display-only");
        return comment;
    }

    private static Arguments commentQuery(final String suffix, final Map<String, String> query,
            final Function<PostCommentService, List<?>> call) {
        return Arguments.of(suffix, query, call);
    }

    static Stream<Arguments> commentQueries() {
        return Stream.of(
                commentQuery("/comments/post/" + COMMENTED_POST_ID, Map.of(),
                        s -> s.getCommentsByPost(COMMENTED_POST_ID)),
                commentQuery("/comments/user/" + COMMENTER_ID, Map.of(), s -> s.getCommentsByUser(COMMENTER_ID)),
                commentQuery("/comments/search", Map.of("query", "thanks"), s -> s.searchComments("thanks")),
                commentQuery("/comments/date-range", Map.of("startDate", "2026-02-01", "endDate", "2026-02-28"),
                        s -> s.getCommentsByDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("commentQueries")
    void commentQueries_mapPayloadAndSendQuery(final String suffix, final Map<String, String> query,
            final Function<PostCommentService, List<?>> call) {
        StubBackend.stub("GET", suffix, 200, "[" + COMMENT_JSON + "]");

        assertThat(call.apply(this.commentService)).singleElement().hasFieldOrPropertyWithValue("postTitle",
                "Release notes");

        final var seen = StubBackend.onlyRequest();
        assertThat(seen.path()).endsWith(suffix);
        assertThat(seen.queryParams()).isEqualTo(query);
    }

    @ParameterizedTest(name = "page={0} size={1}")
    @CsvSource({ "-1,0,0,20", "5,250,5,100", "2,15,2,15" })
    void getAllComments_mapsPayloadAndNormalisesPagination(final int page, final int size, final int sentPage,
            final int sentSize) {
        StubBackend.stub("GET", "/comments", 200, "[" + COMMENT_JSON + "]");

        final var comments = this.commentService.getAllComments(page, size);

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getUsername()).isEqualTo("reader");
        assertThat(StubBackend.onlyRequest().queryParams())
                .isEqualTo(Map.of("page", String.valueOf(sentPage), "size", String.valueOf(sentSize)));
    }

    @Test
    void getCommentById_returnsComment_on200() {
        StubBackend.stub("GET", COMMENT_PATH, 200, COMMENT_JSON);

        final var comment = this.commentService.getCommentById(COMMENT_ID);

        assertThat(comment.getContent()).isEqualTo("Thanks for the update");
        assertThat(comment.getPostPublicId()).isEqualTo(COMMENTED_POST_ID);
        assertThat(StubBackend.onlyRequest().path()).endsWith(COMMENT_PATH);
    }

    /** A 404 on lookup surfaces the service's not-found message. */
    @Test
    void getCommentById_throwsNotFound_on404() {
        this.assertCommentFailure("GET", COMMENT_PATH, 404, null, s -> s.getCommentById(COMMENT_ID),
                "Comment not found");
    }

    @Test
    void createComment_returnsCreatedComment_on201() {
        StubBackend.stub("POST", "/comments", 201, COMMENT_JSON);

        final var created = this.commentService.createComment(fromView());

        assertThat(created.getPublicId()).isEqualTo(COMMENT_ID);
        assertThat(created.getUserPublicId()).isEqualTo(COMMENTER_ID);
        final var seen = StubBackend.onlyRequest();
        assertThat(seen.method()).isEqualTo("POST");
        assertThat(seen.body()).contains("Thanks for the update", COMMENTED_POST_ID).doesNotContain("display-only");
    }

    @Test
    void updateComment_sendsOnlyContent_andReturnsUpdated_on200() {
        StubBackend.stub("PUT", COMMENT_PATH, 200, COMMENT_JSON);

        final var updated = this.commentService.updateComment(fromView());

        assertThat(updated.getUsername()).isEqualTo("reader");
        final var seen = StubBackend.onlyRequest();
        assertThat(seen.path()).endsWith(COMMENT_PATH);
        assertThat(seen.body()).contains("Thanks for the update").doesNotContain(COMMENTED_POST_ID, "display-only");
    }

    @Test
    void deleteComment_succeeds_on204() {
        StubBackend.stub("DELETE", COMMENT_PATH, 204, null);

        this.commentService.deleteComment(COMMENT_ID);

        assertThat(StubBackend.onlyRequest().method()).isEqualTo("DELETE");
    }

    /** A 404 on delete surfaces the service's not-found message. */
    @Test
    void deleteComment_throwsNotFound_on404() {
        this.assertCommentFailure("DELETE", COMMENT_PATH, 404, null, s -> s.deleteComment(COMMENT_ID),
                "Comment not found");
    }

    private static Arguments failedCommentWrite(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostCommentService> call, final String message) {
        return Arguments.of(method, suffix, status, body, call, message);
    }

    static Stream<Arguments> failedCommentWrites() {
        return Stream.of(
                failedCommentWrite("POST", "/comments", 422, COMMENT_VALIDATION_JSON, s -> s.createComment(fromView()),
                        "Failed to create comment: content must not be blank"),
                failedCommentWrite("PUT", COMMENT_PATH, 422, COMMENT_VALIDATION_JSON, s -> s.updateComment(fromView()),
                        "Failed to update comment: content must not be blank"));
    }

    /** 422 validation errors surface the operation's failure prefix. */
    @ParameterizedTest(name = "{0} {2}")
    @MethodSource("failedCommentWrites")
    void commentWrites_failWithPrefixedMessage(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostCommentService> call, final String message) {
        this.assertCommentFailure(method, suffix, status, body, call, message);
    }

    @Test
    void anyCall_logsOutAndThrows_on401() {
        StubBackend.stub("GET", COMMENT_PATH, 401, null);

        assertThatThrownBy(() -> this.commentService.getCommentById(COMMENT_ID))
                .isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
    }

    private void assertCommentFailure(final String method, final String suffix, final int status,
            @Nullable final String body, final Consumer<PostCommentService> call, final String message) {
        StubBackend.stub(method, suffix, status, body);

        assertThatThrownBy(() -> call.accept(this.commentService)).isInstanceOf(ServiceException.class)
                .hasMessage(message);
    }
}
