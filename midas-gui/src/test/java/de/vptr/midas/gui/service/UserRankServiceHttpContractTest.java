package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Pins {@link UserRankService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class UserRankServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String RANK_ID = "01HZX3RANK0000000000000003";
    private static final String RANK_PATH = "/user-ranks/" + RANK_ID;
    private static final String RANK_JSON = """
            {"publicId":"01HZX3RANK0000000000000003","name":"Moderator","usersCount":4,"adminView":true,\
            "pageAdd":true,"pageDelete":false,"pageEdit":true,"userRankAdd":false,\
            "created":"2023-11-05T14:00:00","lastEdit":"2024-04-01T16:45:00"}""";
    private static final String RANKED_USER_JSON = """
            {"publicId":"01HZX3CAROL000000000000001","username":"carol","rankPublicId":"01HZX3RANK0000000000000003",\
            "rankName":"Moderator","banned":false,"activated":true}""";
    private static final String UNRANKED_USER_JSON = """
            {"publicId":"01HZX3DAVE0000000000000001","username":"dave","banned":false,"activated":true}""";
    private static final String RANK_REJECTED = """
            {"status":422,"error":"Unprocessable Entity","message":"Rank name already exists"}""";

    @Inject
    UserRankService rankService;

    @Test
    void getAllRanks_mapsRanksWithoutPagination() {
        StubBackend.stub("GET", "/user-ranks", 200, "[" + RANK_JSON + "]");

        final var ranks = this.rankService.getAllRanks();

        assertThat(ranks).singleElement().satisfies(rank -> {
            assertThat(rank.getName()).isEqualTo("Moderator");
            assertThat(rank.getUsersCount()).isEqualTo(4L);
        });
        assertThat(request("GET", "/user-ranks").query()).isNull();
    }

    @Test
    void getRankById_returnsMappedRank_on200() {
        StubBackend.stub("GET", RANK_PATH, 200, RANK_JSON);

        final var rank = this.rankService.getRankById(RANK_ID);

        assertThat(rank.getPublicId()).isEqualTo(RANK_ID);
        assertThat(rank.getName()).isEqualTo("Moderator");
        request("GET", RANK_PATH);
    }

    static Stream<Arguments> missingRankCalls() {
        return Stream.of(Arguments.of("GET", forRank(s -> s.getRankById(RANK_ID)), "User rank not found"),
                Arguments.of("PUT", forRank(s -> s.updateRank(existingRank())), "Backend request failed"),
                Arguments.of("DELETE", forRank(s -> s.deleteRank(RANK_ID)), "User rank not found"));
    }

    /** A 404 surfaces "User rank not found" where the service carries that message, else the generic failure. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("missingRankCalls")
    void rankCall_reportsMissingRank_on404(final String method, final ThrowingConsumer<UserRankService> call,
            final String message) {
        StubBackend.stub(method, RANK_PATH, 404, """
                {"status":404,"error":"Not Found","message":"User rank not found"}""");

        assertThatThrownBy(() -> call.accept(this.rankService)).isInstanceOf(ServiceException.class)
                .hasMessage(message);
    }

    @Test
    void createRank_postsRankAndMapsCreatedRank_on201() {
        StubBackend.stub("POST", "/user-ranks", 201, RANK_JSON);
        final var rank = new UserRankDto("Moderator");
        rank.setAdminView(true);

        final var created = this.rankService.createRank(rank);

        assertThat(created.getPublicId()).isEqualTo(RANK_ID);
        assertThat(created.isPageEdit()).isTrue();
        assertThat(request("POST", "/user-ranks").body()).contains("\"name\":\"Moderator\"")
                .contains("\"adminView\":true");
    }

    @Test
    void createRank_reportsBackendMessageWithPrefix_on422() {
        StubBackend.stub("POST", "/user-ranks", 422, RANK_REJECTED);

        assertThatThrownBy(() -> this.rankService.createRank(new UserRankDto("Moderator")))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Failed to create user rank: Rank name already exists");
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void updateRank_putsRankToItsId_on200() {
        StubBackend.stub("PUT", RANK_PATH, 200, RANK_JSON);

        final var updated = this.rankService.updateRank(existingRank());

        assertThat(updated.getName()).isEqualTo("Moderator");
        assertThat(request("PUT", RANK_PATH).body()).contains("\"userRankAdd\":true");
    }

    @Test
    void updateRank_reportsBackendMessageWithPrefix_on422() {
        StubBackend.stub("PUT", RANK_PATH, 422, RANK_REJECTED);

        assertThatThrownBy(() -> this.rankService.updateRank(existingRank())).isInstanceOf(ServiceException.class)
                .hasMessage("Failed to update user rank: Rank name already exists");
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void searchRanks_sendsQueryAndMapsResults() {
        StubBackend.stub("GET", "/user-ranks/search", 200, "[" + RANK_JSON + "]");

        final var found = this.rankService.searchRanks("mod");

        assertThat(found).extracting(rank -> rank.getPublicId()).containsExactly(RANK_ID);
        assertThat(request("GET", "/user-ranks/search").query()).isEqualTo("query=mod");
    }

    @Test
    void deleteRank_succeeds_on204() {
        StubBackend.stub("DELETE", RANK_PATH, 204, null);

        assertThatCode(() -> this.rankService.deleteRank(RANK_ID)).doesNotThrowAnyException();

        request("DELETE", RANK_PATH);
    }

    @Test
    void getCurrentUserRank_fetchesCurrentUserThenTheirRank() {
        StubBackend.stub("GET", "/users/me", 200, RANKED_USER_JSON);
        StubBackend.stub("GET", RANK_PATH, 200, RANK_JSON);

        final var rank = this.rankService.getCurrentUserRank();

        assertThat(rank.getName()).isEqualTo("Moderator");
        assertThat(StubBackend.requests()).extracting(StubBackend.Recorded::path).satisfiesExactly(
                first -> assertThat(first).endsWith("/users/me"), second -> assertThat(second).endsWith(RANK_PATH));
    }

    @Test
    void getCurrentUserRank_returnsEmptyRank_whenUserHasNoRank() {
        StubBackend.stub("GET", "/users/me", 200, UNRANKED_USER_JSON);

        assertEmptyRank(this.rankService.getCurrentUserRank());
        request("GET", "/users/me");
    }

    @Test
    void getCurrentUserRank_returnsEmptyRank_whenRankLookupFails() {
        StubBackend.stub("GET", "/users/me", 200, RANKED_USER_JSON);
        StubBackend.stub("GET", RANK_PATH, 404, null);

        assertEmptyRank(this.rankService.getCurrentUserRank());
        assertThat(StubBackend.requests()).hasSize(2);
    }

    /** The service deliberately swallows an AuthenticationException from the rank lookup, after it has logged out. */
    @Test
    void getCurrentUserRank_logsOutButReturnsEmptyRank_whenRankLookupIsUnauthorised() {
        StubBackend.stub("GET", "/users/me", 200, RANKED_USER_JSON);
        StubBackend.stub("GET", RANK_PATH, 401, null);

        assertEmptyRank(this.rankService.getCurrentUserRank());
        verify(this.authService).logout();
        assertThat(StubBackend.requests()).hasSize(2);
    }

    /** A current user the backend does not find yields the empty rank. */
    @Test
    void getCurrentUserRank_returnsEmptyRank_whenCurrentUserNotFound() {
        StubBackend.stub("GET", "/users/me", 404, null);

        assertEmptyRank(this.rankService.getCurrentUserRank());
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void getAllRanks_logsOutAndThrows_on401() {
        StubBackend.stub("GET", "/user-ranks", 401, null);

        assertThatThrownBy(() -> this.rankService.getAllRanks()).isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
        assertThat(StubBackend.requests()).hasSize(1);
    }

    private static ThrowingConsumer<UserRankService> forRank(final ThrowingConsumer<UserRankService> call) {
        return call;
    }

    private static UserRankDto existingRank() {
        final var rank = new UserRankDto("Moderator");
        rank.setPublicId(RANK_ID);
        rank.setUserRankAdd(true);
        return rank;
    }

    /** The empty rank the service returns on failure: a fresh rank with nothing set. */
    private static void assertEmptyRank(final Object rank) {
        assertThat(rank).hasFieldOrPropertyWithValue("publicId", null).hasFieldOrPropertyWithValue("name", null);
    }

    /** Asserts that the single request sent used this method and path suffix, and returns it. */
    private static StubBackend.Recorded request(final String method, final String suffix) {
        final var recorded = StubBackend.onlyRequest();
        assertThat(recorded.method()).isEqualTo(method);
        assertThat(recorded.path()).endsWith(suffix);
        return recorded;
    }
}
