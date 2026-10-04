package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Pins {@link UserGroupService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class UserGroupServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String GROUP_ID = "01HZX3GR0UP000000000000001";
    private static final String MEMBER_ID = "01HZX3MEMBER00000000000001";
    private static final String GROUP_PATH = "/user-groups/" + GROUP_ID;
    private static final String MEMBERSHIP_PATH = GROUP_PATH + "/users/" + MEMBER_ID;
    private static final String GROUP_JSON = """
            {"publicId":"01HZX3GR0UP000000000000001","name":"Editors","userCount":5,\
            "created":"2024-03-01T09:00:00","lastEdit":"2024-03-02T09:00:00"}""";
    private static final String MEMBER_JSON = """
            {"publicId":"01HZX3MEMBER00000000000001","username":"bob","email":"bob@example.com",\
            "rankPublicId":"01HZX3RANK0000000000000002","rankName":"Member","banned":false,"activated":true,\
            "created":"2024-01-20T12:00:00","postsCount":0,"commentsCount":1}""";
    private static final String GROUP_ERROR = """
            {"status":%d,"error":"Rejected","message":"%s"}""";

    @Inject
    UserGroupService groupService;

    static Stream<Arguments> paginations() {
        return Stream.of(Arguments.of(null, null, "page=0", "size=20"), Arguments.of(-1, -5, "page=0", "size=20"),
                Arguments.of(4, 101, "page=4", "size=100"), Arguments.of(0, 1, "page=0", "size=1"));
    }

    @ParameterizedTest
    @MethodSource("paginations")
    void getAllGroups_mapsGroupsAndNormalisesPagination(final Integer page, final Integer size,
            final String expectedPage, final String expectedSize) {
        StubBackend.stub("GET", "/user-groups", 200, "[" + GROUP_JSON + "]");

        final var groups = this.groupService.getAllGroups(page, size);

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.getName()).isEqualTo("Editors");
            assertThat(group.getUserCount()).isEqualTo(5L);
        });
        assertThat(onlyRequestTo("GET", "/user-groups").query().split("&")).containsExactlyInAnyOrder(expectedPage,
                expectedSize);
    }

    @Test
    void getGroupById_returnsMappedGroup_on200() {
        StubBackend.stub("GET", GROUP_PATH, 200, GROUP_JSON);

        final var found = this.groupService.getGroupById(GROUP_ID);

        assertThat(found).hasValueSatisfying(group -> assertThat(group.getPublicId()).isEqualTo(GROUP_ID));
        onlyRequestTo("GET", GROUP_PATH);
    }

    @Test
    void getUsersInGroup_mapsMembers() {
        StubBackend.stub("GET", GROUP_PATH + "/users", 200, "[" + MEMBER_JSON + "]");

        final var members = this.groupService.getUsersInGroup(GROUP_ID);

        assertThat(members).singleElement().satisfies(member -> {
            assertThat(member.getUsername()).isEqualTo("bob");
            assertThat(member.getPublicId()).isEqualTo(MEMBER_ID);
        });
        onlyRequestTo("GET", GROUP_PATH + "/users");
    }

    @Test
    void searchGroups_sendsQueryAndMapsResults() {
        StubBackend.stub("GET", "/user-groups/search", 200, "[" + GROUP_JSON + "]");

        final var found = this.groupService.searchGroups("edit");

        assertThat(found).extracting(group -> group.getName()).containsExactly("Editors");
        assertThat(onlyRequestTo("GET", "/user-groups/search").query()).isEqualTo("query=edit");
    }

    @Test
    void getGroupsByUser_requestsGroupsOfThatUser() {
        StubBackend.stub("GET", "/user-groups/user/" + MEMBER_ID, 200, "[" + GROUP_JSON + "]");

        final var groups = this.groupService.getGroupsByUser(MEMBER_ID);

        assertThat(groups).extracting(group -> group.getPublicId()).containsExactly(GROUP_ID);
        onlyRequestTo("GET", "/user-groups/user/" + MEMBER_ID);
    }

    @Test
    void createGroup_postsNameAndMapsCreatedGroup_on201() {
        StubBackend.stub("POST", "/user-groups", 201, GROUP_JSON);

        final var created = this.groupService.createGroup(new UserGroupDto(null, "Editors"));

        assertThat(created.getPublicId()).isEqualTo(GROUP_ID);
        assertThat(created.getName()).isEqualTo("Editors");
        assertThat(onlyRequestTo("POST", "/user-groups").body()).contains("\"name\":\"Editors\"");
    }

    @Test
    void updateGroup_putsGroupToItsId_on200() {
        StubBackend.stub("PUT", GROUP_PATH, 200, GROUP_JSON.replace("Editors", "Reviewers"));

        final var updated = this.groupService.updateGroup(new UserGroupDto(GROUP_ID, "Reviewers"));

        assertThat(updated.getName()).isEqualTo("Reviewers");
        assertThat(onlyRequestTo("PUT", GROUP_PATH).body()).contains("\"name\":\"Reviewers\"");
    }

    @Test
    void createGroup_reportsBackendMessageWithPrefix_on422() {
        StubBackend.stub("POST", "/user-groups", 422, GROUP_ERROR.formatted(422, "Group name already exists"));

        assertThatThrownBy(() -> this.groupService.createGroup(new UserGroupDto(null, "Editors")))
                .isInstanceOf(ServiceException.class).hasMessage("Failed to create group: Group name already exists");
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void updateGroup_reportsBackendMessageWithPrefix_on422() {
        StubBackend.stub("PUT", GROUP_PATH, 422, GROUP_ERROR.formatted(422, "Name must not be blank"));

        assertThatThrownBy(() -> this.groupService.updateGroup(new UserGroupDto(GROUP_ID, " ")))
                .isInstanceOf(ServiceException.class).hasMessage("Failed to update group: Name must not be blank");
        assertThat(StubBackend.requests()).hasSize(1);
    }

    static Stream<Arguments> voidCalls() {
        return Stream.of(Arguments.of("DELETE", GROUP_PATH, 204, ask(s -> s.deleteGroup(GROUP_ID))),
                Arguments.of("POST", MEMBERSHIP_PATH, 201, ask(s -> s.addUserToGroup(GROUP_ID, MEMBER_ID))),
                Arguments.of("DELETE", MEMBERSHIP_PATH, 204, ask(s -> s.removeUserFromGroup(GROUP_ID, MEMBER_ID))));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("voidCalls")
    void voidCall_succeeds_onDocumentedStatus(final String method, final String suffix, final int status,
            final Consumer<UserGroupService> call) {
        StubBackend.stub(method, suffix, status, null);

        assertThatCode(() -> call.accept(this.groupService)).doesNotThrowAnyException();
        assertThat(onlyRequestTo(method, suffix).body()).isEmpty();
    }

    static Stream<Arguments> notFoundCalls() {
        return Stream.of(Arguments.of("PUT", GROUP_PATH, ask(s -> s.updateGroup(new UserGroupDto(GROUP_ID, "X")))),
                Arguments.of("DELETE", GROUP_PATH, ask(s -> s.deleteGroup(GROUP_ID))),
                Arguments.of("POST", MEMBERSHIP_PATH, ask(s -> s.addUserToGroup(GROUP_ID, MEMBER_ID))),
                Arguments.of("DELETE", MEMBERSHIP_PATH, ask(s -> s.removeUserFromGroup(GROUP_ID, MEMBER_ID))));
    }

    /** A 404 on getGroupById means the group does not exist. */
    @Test
    void getGroupById_returnsEmpty_on404() {
        StubBackend.stub("GET", GROUP_PATH, 404, GROUP_ERROR.formatted(404, "Not found"));

        assertThat(this.groupService.getGroupById(GROUP_ID)).isEmpty();
        assertThat(StubBackend.requests()).hasSize(1);
    }

    /** Writes carry no not-found message, so a 404 surfaces as the generic failure. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("notFoundCalls")
    void throwsGenericFailure_on404(final String method, final String suffix, final Consumer<UserGroupService> call) {
        StubBackend.stub(method, suffix, 404, GROUP_ERROR.formatted(404, "Not found"));

        assertThatThrownBy(() -> call.accept(this.groupService)).isInstanceOf(ServiceException.class)
                .hasMessage("Backend request failed");
        assertThat(StubBackend.requests()).hasSize(1);
    }

    /** The one 4xx the service handles itself: it catches the client's WebApplicationException for 409. */
    @Test
    void addUserToGroup_reportsExistingMembership_on409() {
        StubBackend.stub("POST", MEMBERSHIP_PATH, 409, GROUP_ERROR.formatted(409, "User is already in this group"));

        assertThatThrownBy(() -> this.groupService.addUserToGroup(GROUP_ID, MEMBER_ID))
                .isInstanceOf(ServiceException.class).hasMessage("User is already a member of this group");
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void deleteGroup_logsOutAndThrows_on401() {
        StubBackend.stub("DELETE", GROUP_PATH, 401, null);

        assertThatThrownBy(() -> this.groupService.deleteGroup(GROUP_ID)).isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
        assertThat(StubBackend.requests()).hasSize(1);
    }

    private static Consumer<UserGroupService> ask(final Consumer<UserGroupService> call) {
        return call;
    }

    /** Asserts that exactly one request went out, with this method and path suffix, and returns it. */
    private static StubBackend.Recorded onlyRequestTo(final String method, final String suffix) {
        final var request = StubBackend.onlyRequest();
        assertThat(request.method()).isEqualTo(method);
        assertThat(request.path()).endsWith(suffix);
        return request;
    }
}
