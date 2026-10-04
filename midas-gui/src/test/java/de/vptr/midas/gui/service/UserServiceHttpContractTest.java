package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.midas.gui.dto.PasswordChangeDto;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Pins {@link UserService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class UserServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String USER_ID = "01HZX3U5ER0000000000000001";
    private static final String USER_PATH = "/users/" + USER_ID;
    private static final String USER_JSON = """
            {"publicId":"01HZX3U5ER0000000000000001","username":"alice","email":"alice@example.com",\
            "rankPublicId":"01HZX3RANK0000000000000001","rankName":"Admin","banned":false,"activated":true,\
            "created":"2024-01-15T10:30:00","lastLogin":"2024-02-01T08:00:00","postsCount":3,"commentsCount":7,\
            "isNegative":false,"canManage":false}""";
    private static final String USER_NOT_FOUND = """
            {"status":404,"error":"Not Found","message":"User not found"}""";
    private static final String USERNAME_TAKEN = """
            {"status":422,"error":"Unprocessable Entity","message":"Username already taken"}""";

    @Inject
    UserService userService;

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = { "null, null, 0, 20", "-3, 0, 0, 20", "2, 500, 2, 100", "1, 50, 1, 50" })
    void getAllUsers_sendsNormalisedPagination(final Integer page, final Integer size, final int expectedPage,
            final int expectedSize) {
        StubBackend.stub("GET", "/users", 200, "[" + USER_JSON + "]");

        final var users = this.userService.getAllUsers(page, size);

        assertThat(users).singleElement().satisfies(user -> assertThat(user.getUsername()).isEqualTo("alice"));
        assertThat(sent("GET", "/users").query()).contains("page=" + expectedPage).contains("size=" + expectedSize);
    }

    static Stream<Arguments> lookups() {
        return Stream.of(Arguments.of("getCurrentUser", "/users/me"), Arguments.of("getUserById", USER_PATH),
                Arguments.of("getUserByUsername", "/users/username/alice"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lookups")
    void lookup_returnsMappedUser_on200(final String operation, final String suffix) {
        StubBackend.stub("GET", suffix, 200, USER_JSON);

        final var found = switch (operation) {
            case "getCurrentUser" -> this.userService.getCurrentUser();
            case "getUserById" -> this.userService.getUserById(USER_ID);
            default -> this.userService.getUserByUsername("alice");
        };

        assertThat(found).hasValueSatisfying(user -> {
            assertThat(user.getPublicId()).isEqualTo(USER_ID);
            assertThat(user.getRankPublicId()).isEqualTo("01HZX3RANK0000000000000001");
            assertThat(user.getPostsCount()).isEqualTo(3L);
        });
        sent("GET", suffix);
    }

    static Stream<Arguments> notFoundLookups() {
        return Stream.of(Arguments.of("/users/me", (Function<UserService, Optional<?>>) UserService::getCurrentUser),
                Arguments.of(USER_PATH, (Function<UserService, Optional<?>>) s -> s.getUserById(USER_ID)),
                Arguments.of("/users/username/ghost",
                        (Function<UserService, Optional<?>>) s -> s.getUserByUsername("ghost")));
    }

    /** A 404 on a lookup means the user does not exist. */
    @ParameterizedTest(name = "GET {0}")
    @MethodSource("notFoundLookups")
    void lookup_returnsEmpty_on404(final String suffix, final Function<UserService, Optional<?>> call) {
        StubBackend.stub("GET", suffix, 404, USER_NOT_FOUND);

        assertThat(call.apply(this.userService)).isEmpty();
    }

    static Stream<Arguments> notFoundCalls() {
        return Stream.of(Arguments.of("PUT", USER_PATH, call(s -> s.updateUser(existingUser()))),
                Arguments.of("PATCH", USER_PATH, call(s -> s.updateUserPassword(existingUser()))),
                Arguments.of("DELETE", USER_PATH, call(s -> s.deleteUser(USER_ID))));
    }

    /** Writes carry no not-found message, so a 404 surfaces as the generic failure. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("notFoundCalls")
    void throwsGenericFailure_on404(final String method, final String suffix, final Consumer<UserService> call) {
        StubBackend.stub(method, suffix, 404, USER_NOT_FOUND);

        assertThatThrownBy(() -> call.accept(this.userService)).isInstanceOf(ServiceException.class)
                .hasMessage("Backend request failed");
    }

    @Test
    void createUser_postsUserAndMapsCreatedUser_on201() {
        StubBackend.stub("POST", "/users", 201, USER_JSON);
        final var user = new UserDto("alice", "alice@example.com");
        user.setPassword("Secret#123");

        final var created = this.userService.createUser(user);

        assertThat(created.getPublicId()).isEqualTo(USER_ID);
        assertThat(created.getRankName()).isEqualTo("Admin");
        assertThat(sent("POST", "/users").body()).contains("\"username\":\"alice\"")
                .contains("\"email\":\"alice@example.com\"").contains("\"password\":\"Secret#123\"");
    }

    @Test
    void updateUser_putsUserToItsId_on200() {
        StubBackend.stub("PUT", USER_PATH, 200, USER_JSON);

        final var updated = this.userService.updateUser(existingUser());

        assertThat(updated.getEmail()).isEqualTo("alice@example.com");
        assertThat(sent("PUT", USER_PATH).body()).contains("\"username\":\"alice\"");
    }

    @Test
    void updateUserPassword_patchesUserWithNewPassword_on200() {
        StubBackend.stub("PATCH", USER_PATH, 200, USER_JSON);

        final var updated = this.userService.updateUserPassword(existingUser());

        assertThat(updated.getUsername()).isEqualTo("alice");
        assertThat(sent("PATCH", USER_PATH).body()).contains("\"password\":\"N3w#Secret\"");
    }

    @Test
    void createUser_reportsBackendMessageWithPrefix_on422() {
        StubBackend.stub("POST", "/users", 422, USERNAME_TAKEN);

        assertThatThrownBy(() -> this.userService.createUser(existingUser())).isInstanceOf(ServiceException.class)
                .hasMessage("Failed to create user: Username already taken");
    }

    @Test
    void updateUser_reportsBackendMessageWithPrefix_on422() {
        StubBackend.stub("PUT", USER_PATH, 422, USERNAME_TAKEN);

        assertThatThrownBy(() -> this.userService.updateUser(existingUser())).isInstanceOf(ServiceException.class)
                .hasMessage("Failed to update user: Username already taken");
    }

    @Test
    void deleteUser_succeeds_on204() {
        StubBackend.stub("DELETE", USER_PATH, 204, null);

        assertThatCode(() -> this.userService.deleteUser(USER_ID)).doesNotThrowAnyException();
        sent("DELETE", USER_PATH);
    }

    @Test
    void searchUsers_sendsQueryAndMapsResults() {
        StubBackend.stub("GET", "/users/search", 200, "[" + USER_JSON + "]");

        final var found = this.userService.searchUsers("ali");

        assertThat(found).extracting(user -> user.getUsername()).containsExactly("alice");
        assertThat(sent("GET", "/users/search").query()).isEqualTo("query=ali");
    }

    @Test
    void changePassword_postsBothPasswords_on200() {
        StubBackend.stub("POST", "/users/me/password", 200, null);

        this.userService.changePassword(new PasswordChangeDto("Old#Secret1", "N3w#Secret"));

        assertThat(sent("POST", "/users/me/password").body()).contains("\"currentPassword\":\"Old#Secret1\"")
                .contains("\"newPassword\":\"N3w#Secret\"");
    }

    /** Unlike the writes above, changePassword passes no failure prefix, so the backend message arrives bare. */
    @Test
    void changePassword_reportsBareBackendMessage_on422() {
        StubBackend.stub("POST", "/users/me/password", 422, """
                {"status":422,"error":"Unprocessable Entity","message":"Current password is incorrect"}""");

        assertThatThrownBy(() -> this.userService.changePassword(new PasswordChangeDto("wrong", "N3w#Secret")))
                .isInstanceOf(ServiceException.class).hasMessage("Current password is incorrect");
    }

    @Test
    void getCurrentUser_logsOutAndThrows_on401() {
        StubBackend.stub("GET", "/users/me", 401, null);

        assertThatThrownBy(() -> this.userService.getCurrentUser()).isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
    }

    private static Consumer<UserService> call(final Consumer<UserService> call) {
        return call;
    }

    private static UserDto existingUser() {
        final var user = new UserDto("alice", "alice@example.com");
        user.setPublicId(USER_ID);
        user.setPassword("N3w#Secret");
        return user;
    }

    /** Asserts that exactly one request with this method and path suffix was sent, and returns it. */
    private static StubBackend.Recorded sent(final String method, final String suffix) {
        final var request = StubBackend.onlyRequest();
        assertThat(request.method()).isEqualTo(method);
        assertThat(request.path()).endsWith(suffix);
        return request;
    }
}
