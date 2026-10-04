package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Pins {@link AccountService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class AccountServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String ACCOUNT_ID = "01JQ4ACC0UNT00000000000001";
    private static final String USER_ID = "01JQ4USER00000000000000001";
    private static final String PAYMENT_ID = "01JQ4PAYMENT00000000000001";
    private static final String GENERIC_FAILURE = "Backend request failed";
    private static final String VALIDATION_MESSAGE = "name must not be blank";

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 3, 31);
    private static final Map<String, String> RANGE = Map.of("startDate", "2026-01-01", "endDate", "2026-03-31");

    private static final String ACCOUNT_JSON = """
            {"publicId":"%s","name":"Cash","userCount":1,"associatedUserPublicIds":["%s"],"balance":12.5,\
            "isNegative":false,"canManage":true,"created":"2026-01-02T03:04:05","lastEdit":"2026-01-02T03:04:05"}"""
            .formatted(ACCOUNT_ID, USER_ID);
    private static final String PAYMENT_JSON = """
            {"publicId":"%s","targetAccountPublicId":"%s","targetAccountName":"Cash",\
            "sourceAccountPublicId":"01JQ4S0URCE000000000000001","sourceAccountName":"Bank","userPublicId":"%s",\
            "username":"member","comment":"Groceries","date":"2026-03-10","amount":42.5,\
            "created":"2026-03-10T12:15:50","lastEdit":"2026-03-10T12:15:50"}""".formatted(PAYMENT_ID, ACCOUNT_ID,
            USER_ID);
    private static final String USER_JSON = """
            {"publicId":"%s","username":"member","email":"member@example.com",\
            "rankPublicId":"01JQ4RANK00000000000000001","rankName":"Member","banned":false,"activated":true,\
            "created":"2026-01-02T03:04:05","lastLogin":"2026-03-10T12:15:50","postsCount":0,"commentsCount":0,\
            "isNegative":false,"canManage":true}""".formatted(USER_ID);
    private static final String BALANCE_JSON = """
            {"balance":12.5}""";
    private static final String HISTORY_JSON = """
            [{"date":"2026-03-10","balance":12.5}]""";
    private static final String ACCOUNT_NOT_FOUND_JSON = """
            {"status":404,"error":"Not Found","message":"Account not found"}""";
    private static final String VALIDATION_JSON = """
            {"status":422,"error":"Unprocessable Entity","message":"%s"}""".formatted(VALIDATION_MESSAGE);

    private static final String ACCOUNT_PATH = "/accounts/" + ACCOUNT_ID;
    private static final String MY_ACCOUNT_PATH = "/accounts/my/" + ACCOUNT_ID;
    private static final String MEMBER_PATH = ACCOUNT_PATH + "/users/" + USER_ID;
    private static final String MY_PAYMENTS_PATH = MY_ACCOUNT_PATH + "/payments";
    private static final String MY_PAYMENT_PATH = MY_PAYMENTS_PATH + "/" + PAYMENT_ID;

    @Inject
    AccountService accountService;

    private static Map<String, String> withRange(final String... pairs) {
        final var merged = new HashMap<>(RANGE);
        for (int i = 0; i < pairs.length; i += 2) {
            merged.put(pairs[i], pairs[i + 1]);
        }
        return merged;
    }

    private static Arguments read(final String suffix, final String json, final Function<AccountService, Object> probe,
            final Object expected, final Map<String, String> query) {
        return arguments(suffix, json, probe, expected, query);
    }

    static Stream<Arguments> readEndpoints() {
        final String accounts = "[" + ACCOUNT_JSON + "]";
        final String payments = "[" + PAYMENT_JSON + "]";
        return Stream.of(
                read("/accounts", accounts, s -> s.getAllAccounts(null, null).get(0).getName(), "Cash",
                        Map.of("page", "0", "size", "20")),
                read("/accounts", accounts, s -> s.getAllAccounts(-3, 500).get(0).getName(), "Cash",
                        Map.of("page", "0", "size", "100")),
                read(ACCOUNT_PATH, ACCOUNT_JSON, s -> s.getAccountById(ACCOUNT_ID).orElseThrow().getName(), "Cash",
                        Map.of()),
                read("/accounts/name/Cash", ACCOUNT_JSON, s -> s.getAccountByName("Cash").orElseThrow().getPublicId(),
                        ACCOUNT_ID, Map.of()),
                read("/accounts/user/" + USER_ID, accounts, s -> s.getAccountsByUser(USER_ID).get(0).getPublicId(),
                        ACCOUNT_ID, Map.of()),
                read("/accounts/my", accounts, s -> s.getMyAccounts().get(0).getName(), "Cash", Map.of()),
                read("/accounts/my/with-balances", accounts,
                        s -> s.getMyAccountsWithBalances(START, END).get(0).getBalance().toPlainString(), "12.5",
                        withRange("page", "0", "size", "100")),
                read("/accounts/my/with-balances", accounts,
                        s -> s.getMyAccountsWithBalances(null, null).get(0).getName(), "Cash",
                        Map.of("page", "0", "size", "100")),
                read("/accounts/search", accounts, s -> s.searchAccounts("Cash").get(0).getPublicId(), ACCOUNT_ID,
                        Map.of("query", "Cash")),
                read(ACCOUNT_PATH + "/payments/outgoing", payments,
                        s -> s.getOutgoingPayments(ACCOUNT_ID).get(0).getComment(), "Groceries", Map.of()),
                read(ACCOUNT_PATH + "/payments/incoming", payments,
                        s -> s.getIncomingPayments(ACCOUNT_ID).get(0).getAmount().toPlainString(), "42.5", Map.of()),
                read(ACCOUNT_PATH + "/users", "[" + USER_JSON + "]",
                        s -> s.getAssociatedUsers(ACCOUNT_ID).get(0).getUsername(), "member", Map.of()),
                read(ACCOUNT_PATH + "/balance", BALANCE_JSON, s -> s.getAccountBalance(ACCOUNT_ID).toPlainString(),
                        "12.5", Map.of()),
                read(MY_ACCOUNT_PATH + "/balance", BALANCE_JSON,
                        s -> s.getMyAccountBalance(ACCOUNT_ID, START, END).toPlainString(), "12.5", RANGE),
                read(MY_ACCOUNT_PATH + "/balance", BALANCE_JSON,
                        s -> s.getMyAccountBalance(ACCOUNT_ID, null, null).toPlainString(), "12.5", Map.of()),
                read(MY_ACCOUNT_PATH + "/balance-history", HISTORY_JSON,
                        s -> s.getMyAccountBalanceHistory(ACCOUNT_ID, START, END).get(0).getDate(),
                        LocalDate.of(2026, 3, 10), RANGE),
                read("/accounts/my/balance-history", HISTORY_JSON,
                        s -> s.getMyTotalBalanceHistory(null, null).get(0).getBalance().toPlainString(), "12.5",
                        Map.of()),
                read("/accounts/my/balance-history", HISTORY_JSON,
                        s -> s.getMyTotalBalanceHistory(START, END).get(0).getDate(), LocalDate.of(2026, 3, 10), RANGE),
                read(MY_PAYMENTS_PATH, payments,
                        s -> s.getMyAccountPayments(ACCOUNT_ID, START, END, "groceries", 2, 0).get(0).getPublicId(),
                        PAYMENT_ID, withRange("page", "2", "size", "20", "query", "groceries")),
                read(MY_PAYMENTS_PATH, payments,
                        s -> s.getMyAccountPayments(ACCOUNT_ID, null, null, null, null, 250).get(0).getComment(),
                        "Groceries", Map.of("page", "0", "size", "100")),
                read("/accounts/my/payments", "{\"" + ACCOUNT_ID + "\":" + payments + "}",
                        s -> s.getMyAccountsPayments().get(ACCOUNT_ID).get(0).getPublicId(), PAYMENT_ID, Map.of()),
                read(MY_PAYMENTS_PATH + "/outgoing", payments,
                        s -> s.getMyAccountOutgoingPayments(ACCOUNT_ID).get(0).getSourceAccountName(), "Bank",
                        Map.of()),
                read(MY_PAYMENTS_PATH + "/incoming", payments,
                        s -> s.getMyAccountIncomingPayments(ACCOUNT_ID).get(0).getTargetAccountName(), "Cash",
                        Map.of()));
    }

    @ParameterizedTest(name = "GET {0} {4}")
    @MethodSource("readEndpoints")
    void readEndpoint_mapsPayloadAndSendsQuery(final String suffix, final String json,
            final Function<AccountService, Object> probe, final Object expected, final Map<String, String> query) {
        StubBackend.stub("GET", suffix, 200, json);

        assertThat(probe.apply(this.accountService)).isEqualTo(expected);

        final var request = StubBackend.onlyRequest();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.path()).endsWith(suffix);
        assertThat(request.queryParams()).isEqualTo(query);
    }

    @Test
    void createAccount_returnsCreatedAccount_on201() {
        StubBackend.stub("POST", "/accounts", 201, ACCOUNT_JSON);

        final var created = this.accountService.createAccount(new AccountDto("Cash"));

        assertThat(created.getPublicId()).isEqualTo(ACCOUNT_ID);
        assertThat(created.getName()).isEqualTo("Cash");
        assertThat(StubBackend.onlyRequest().jsonBody().get("name").asText()).isEqualTo("Cash");
    }

    @Test
    void updateAccount_returnsUpdatedAccount_on200() {
        StubBackend.stub("PUT", ACCOUNT_PATH, 200, ACCOUNT_JSON);
        final var account = new AccountDto("Cash");
        account.setPublicId(ACCOUNT_ID);

        final var updated = this.accountService.updateAccount(account);

        assertThat(updated.getName()).isEqualTo("Cash");
        final var request = StubBackend.onlyRequest();
        assertThat(request.path()).endsWith(ACCOUNT_PATH);
        assertThat(request.jsonBody().get("name").asText()).isEqualTo("Cash");
    }

    @Test
    void updateUserAccountMeta_sendsFlags_andSucceedsOn204() {
        StubBackend.stub("PATCH", MEMBER_PATH, 204, null);

        this.accountService.updateUserAccountMeta(ACCOUNT_ID, USER_ID, true, false);

        final var body = StubBackend.onlyRequest().jsonBody();
        assertThat(body.get("isNegative").asBoolean()).isTrue();
        assertThat(body.get("canManage").asBoolean()).isFalse();
    }

    private static PaymentDto groceries() {
        return new PaymentDto(null, ACCOUNT_ID, new BigDecimal("42.5"), LocalDate.of(2026, 3, 10), "Groceries", null,
                null);
    }

    @Test
    void createMyAccountPayment_returnsCreatedPayment_on201() {
        StubBackend.stub("POST", MY_PAYMENTS_PATH, 201, PAYMENT_JSON);

        final var created = this.accountService.createMyAccountPayment(ACCOUNT_ID, groceries());

        assertThat(created.getPublicId()).isEqualTo(PAYMENT_ID);
        assertThat(created.getDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        final var body = StubBackend.onlyRequest().jsonBody();
        assertThat(body.get("date").asText()).isEqualTo("2026-03-10");
        assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("42.5");
        assertThat(body.get("targetAccountPublicId").asText()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void updateMyAccountPayment_returnsUpdatedPayment_on200() {
        StubBackend.stub("PUT", MY_PAYMENT_PATH, 200, PAYMENT_JSON);

        final var updated = this.accountService.updateMyAccountPayment(ACCOUNT_ID, PAYMENT_ID, groceries());

        assertThat(updated.getAmount()).isEqualByComparingTo("42.5");
        final var request = StubBackend.onlyRequest();
        assertThat(request.path()).endsWith(MY_PAYMENT_PATH);
        assertThat(request.jsonBody().get("comment").asText()).isEqualTo("Groceries");
    }

    static Stream<Arguments> voidEndpoints() {
        return Stream.of(
                arguments("DELETE", ACCOUNT_PATH, 204, (Consumer<AccountService>) s -> s.deleteAccount(ACCOUNT_ID)),
                arguments("POST", MEMBER_PATH, 201,
                        (Consumer<AccountService>) s -> s.associateUserWithAccount(ACCOUNT_ID, USER_ID)),
                arguments("DELETE", MEMBER_PATH, 204,
                        (Consumer<AccountService>) s -> s.removeUserFromAccount(ACCOUNT_ID, USER_ID)),
                arguments("DELETE", MY_PAYMENT_PATH, 204,
                        (Consumer<AccountService>) s -> s.deleteMyAccountPayment(ACCOUNT_ID, PAYMENT_ID)));
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @MethodSource("voidEndpoints")
    void voidEndpoint_succeeds_onExpectedStatus(final String method, final String suffix, final int status,
            final Consumer<AccountService> call) {
        StubBackend.stub(method, suffix, status, null);

        assertThatCode(() -> call.accept(this.accountService)).doesNotThrowAnyException();
        final var request = StubBackend.onlyRequest();
        assertThat(request.method()).isEqualTo(method);
        assertThat(request.path()).endsWith(suffix);
    }

    static Stream<Arguments> entityWriteEndpoints() {
        return Stream.of(
                arguments("POST", "/accounts", 201, "Failed to create account",
                        (Consumer<AccountService>) s -> s.createAccount(new AccountDto("Cash"))),
                arguments("PUT", ACCOUNT_PATH, 200, "Failed to update account", (Consumer<AccountService>) s -> {
                    final var account = new AccountDto("Cash");
                    account.setPublicId(ACCOUNT_ID);
                    s.updateAccount(account);
                }),
                arguments("POST", MY_PAYMENTS_PATH, 201, "Failed to create payment",
                        (Consumer<AccountService>) s -> s.createMyAccountPayment(ACCOUNT_ID, groceries())),
                arguments("PUT", MY_PAYMENT_PATH, 200, "Failed to update payment",
                        (Consumer<AccountService>) s -> s.updateMyAccountPayment(ACCOUNT_ID, PAYMENT_ID, groceries())));
    }

    @ParameterizedTest(name = "{0} {1} -> 422")
    @MethodSource("entityWriteEndpoints")
    void entityWriteEndpoint_reportsValidationMessage_on422(final String method, final String suffix, final int status,
            final String prefix, final Consumer<AccountService> call) {
        StubBackend.stub(method, suffix, 422, VALIDATION_JSON);

        assertThatThrownBy(() -> call.accept(this.accountService)).as("instead of %s", status)
                .isInstanceOf(ServiceException.class).hasMessage(prefix + ": " + VALIDATION_MESSAGE);
        assertThat(StubBackend.requests()).hasSize(1);
    }

    static Stream<Arguments> notFoundLookups() {
        return Stream.of(
                arguments(ACCOUNT_PATH, (Function<AccountService, Optional<?>>) s -> s.getAccountById(ACCOUNT_ID)),
                arguments("/accounts/name/Cash",
                        (Function<AccountService, Optional<?>>) s -> s.getAccountByName("Cash")));
    }

    /** A 404 on a lookup means the account does not exist. */
    @ParameterizedTest(name = "GET {0} -> 404")
    @MethodSource("notFoundLookups")
    void notFoundLookup_returnsEmpty(final String suffix, final Function<AccountService, Optional<?>> call) {
        StubBackend.stub("GET", suffix, 404, ACCOUNT_NOT_FOUND_JSON);

        assertThat(call.apply(this.accountService)).isEmpty();
        assertThat(StubBackend.requests()).hasSize(1);
    }

    static Stream<Arguments> notFoundWrites() {
        return Stream.of(arguments(ACCOUNT_PATH, (Consumer<AccountService>) s -> s.deleteAccount(ACCOUNT_ID)),
                arguments(MEMBER_PATH, (Consumer<AccountService>) s -> s.removeUserFromAccount(ACCOUNT_ID, USER_ID)),
                arguments(MY_PAYMENT_PATH,
                        (Consumer<AccountService>) s -> s.deleteMyAccountPayment(ACCOUNT_ID, PAYMENT_ID)));
    }

    /** These deletes carry no not-found message, so a 404 surfaces as the generic failure. */
    @ParameterizedTest(name = "DELETE {0} -> 404")
    @MethodSource("notFoundWrites")
    void notFoundDelete_throwsGenericFailure(final String suffix, final Consumer<AccountService> call) {
        StubBackend.stub("DELETE", suffix, 404, ACCOUNT_NOT_FOUND_JSON);

        assertThatThrownBy(() -> call.accept(this.accountService)).isInstanceOf(ServiceException.class)
                .hasMessage(GENERIC_FAILURE);
        assertThat(StubBackend.requests()).hasSize(1);
    }

    /** A 403 has no dedicated message and surfaces as the generic failure. */
    @Test
    void getMyAccountBalance_throwsGenericFailure_on403() {
        StubBackend.stub("GET", MY_ACCOUNT_PATH + "/balance", 403, null);

        assertThatThrownBy(() -> this.accountService.getMyAccountBalance(ACCOUNT_ID, START, END))
                .isInstanceOf(ServiceException.class).hasMessage(GENERIC_FAILURE);
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void associateUserWithAccount_reportsDuplicate_on409() {
        StubBackend.stub("POST", MEMBER_PATH, 409, """
                {"status":409,"error":"Conflict","message":"Duplicate association"}""");

        assertThatThrownBy(() -> this.accountService.associateUserWithAccount(ACCOUNT_ID, USER_ID))
                .isInstanceOf(ServiceException.class).hasMessage("User is already associated with this account");
        assertThat(StubBackend.requests()).hasSize(1);
    }

    /** Only a 409 gets the duplicate message; other failures stay generic. */
    @Test
    void associateUserWithAccount_reportsGenericFailure_onOtherStatus() {
        StubBackend.stub("POST", MEMBER_PATH, 500, null);

        assertThatThrownBy(() -> this.accountService.associateUserWithAccount(ACCOUNT_ID, USER_ID))
                .isInstanceOf(ServiceException.class).hasMessage(GENERIC_FAILURE);
        assertThat(StubBackend.requests()).hasSize(1);
    }

    /** A 200 without a body or without the balance field leaves nothing to show. */
    @ParameterizedTest(name = "body: {0}")
    @NullSource
    @ValueSource(strings = "{}")
    void getAccountBalance_rejectsPayloadWithoutBalance(@Nullable final String body) {
        StubBackend.stub("GET", ACCOUNT_PATH + "/balance", 200, body);

        assertThatThrownBy(() -> this.accountService.getAccountBalance(ACCOUNT_ID)).isInstanceOf(ServiceException.class)
                .hasMessage("Malformed balance payload: missing balance");
    }

    /**
     * A body that is not JSON fails to deserialise. The REST client reports that as a WebApplicationException carrying
     * the 200, so it surfaces as the generic failure rather than as a raw exception.
     */
    @Test
    void getAccountBalance_reportsGenericFailure_forPayloadThatIsNotJson() {
        StubBackend.stub("GET", ACCOUNT_PATH + "/balance", 200, "not-json");

        assertThatThrownBy(() -> this.accountService.getAccountBalance(ACCOUNT_ID)).isInstanceOf(ServiceException.class)
                .hasMessage(GENERIC_FAILURE).hasRootCauseInstanceOf(JsonProcessingException.class);
        // A body that does not parse is not a connection failure, so it is not retried.
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void anyCall_logsOutAndThrows_on401() {
        StubBackend.stub("GET", "/accounts/my", 401, null);

        assertThatThrownBy(() -> this.accountService.getMyAccounts()).isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
        assertThat(StubBackend.requests()).hasSize(1);
    }
}
