package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.AccountClient;
import de.vptr.midas.gui.client.PageClient;
import de.vptr.midas.gui.client.PaymentClient;
import de.vptr.midas.gui.client.PostCategoryClient;
import de.vptr.midas.gui.client.PostClient;
import de.vptr.midas.gui.client.PostCommentClient;
import de.vptr.midas.gui.client.UserClient;
import de.vptr.midas.gui.client.UserGroupClient;
import de.vptr.midas.gui.client.UserRankClient;
import de.vptr.midas.gui.exception.ServiceException;
import jakarta.ws.rs.ProcessingException;

/** Every service reports an unreachable backend the same way; the stub backend cannot simulate that cheaply. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway")
class ServiceConnectionFailureTest {

    private final ProcessingException refused = new ProcessingException("Connection refused");

    @Mock
    AuthService authService;

    @Mock
    @RestClient
    AccountClient accountClient;

    @Mock
    @RestClient
    PageClient pageClient;

    @Mock
    @RestClient
    PaymentClient paymentClient;

    @Mock
    @RestClient
    PostClient postClient;

    @Mock
    @RestClient
    PostCategoryClient categoryClient;

    @Mock
    @RestClient
    PostCommentClient commentClient;

    @Mock
    @RestClient
    UserClient userClient;

    @Mock
    @RestClient
    UserGroupClient groupClient;

    @Mock
    @RestClient
    UserRankClient userRankClient;

    @InjectMocks
    AccountService accountService;

    @InjectMocks
    PageService pageService;

    @InjectMocks
    PaymentService paymentService;

    @InjectMocks
    PostService postService;

    @InjectMocks
    PostCategoryService categoryService;

    @InjectMocks
    PostCommentService commentService;

    @InjectMocks
    UserService userService;

    @InjectMocks
    UserGroupService groupService;

    @InjectMocks
    UserRankService userRankService;

    private static Arguments call(final String name, final Consumer<ServiceConnectionFailureTest> call) {
        return arguments(name, call);
    }

    static Stream<Arguments> calls() {
        return Stream.of(call("accounts", t -> {
            when(t.accountClient.getMyAccounts()).thenThrow(t.refused);
            t.accountService.getMyAccounts();
        }), call("pages", t -> {
            when(t.pageClient.getPage("1")).thenThrow(t.refused);
            t.pageService.getPageById("1");
        }), call("payments", t -> {
            when(t.paymentClient.getPayment("1")).thenThrow(t.refused);
            t.paymentService.getPaymentById("1");
        }), call("posts", t -> {
            when(t.postClient.getPost("1")).thenThrow(t.refused);
            t.postService.getPostById("1");
        }), call("categories", t -> {
            when(t.categoryClient.getRootCategories()).thenThrow(t.refused);
            t.categoryService.getRootCategories();
        }), call("comments", t -> {
            when(t.commentClient.getComment("1")).thenThrow(t.refused);
            t.commentService.getCommentById("1");
        }), call("users", t -> {
            when(t.userClient.getCurrentUser()).thenThrow(t.refused);
            t.userService.getCurrentUser();
        }), call("groups", t -> {
            when(t.groupClient.getGroup("1")).thenThrow(t.refused);
            t.groupService.getGroupById("1");
        }), call("ranks", t -> {
            when(t.userRankClient.getRank("1")).thenThrow(t.refused);
            t.userRankService.getRankById("1");
        }));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calls")
    void reportsBackendConnectionFailure(final String name, final Consumer<ServiceConnectionFailureTest> call) {
        assertThatThrownBy(() -> call.accept(this)).isInstanceOf(ServiceException.class)
                .hasMessage("Backend connection failed").hasCause(this.refused);
    }
}
