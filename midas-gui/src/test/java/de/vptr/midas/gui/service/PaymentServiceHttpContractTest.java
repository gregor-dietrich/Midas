package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.AbstractHttpContractTest;
import de.vptr.midas.gui.util.StubBackend;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Pins {@link PaymentService} behaviour against real HTTP responses from a stub backend. */
@QuarkusTest
@QuarkusTestResource(value = StubBackend.class, restrictToAnnotatedClass = true)
@SuppressWarnings("NullAway")
class PaymentServiceHttpContractTest extends AbstractHttpContractTest {

    private static final String PAYMENT_ID = "01JQ5PAYMENT00000000000007";
    private static final String ACCOUNT_ID = "01JQ5ACC0UNT00000000000007";
    private static final String USER_ID = "01JQ5USER00000000000000007";
    private static final String PAYMENT_PATH = "/payments/" + PAYMENT_ID;
    private static final String PAYMENT_JSON = payment(PAYMENT_ID, "2026-02-14");
    private static final String PAYMENTS_JSON = "[" + PAYMENT_JSON + "]";
    private static final String REJECTION_JSON = """
            {"status":422,"error":"Unprocessable Entity","message":"amount must be positive"}""";
    private static final String NOT_FOUND_JSON = """
            {"status":404,"error":"Not Found","message":"Payment not found"}""";
    private static final String GENERIC_FAILURE = "Backend request failed";

    @Inject
    PaymentService paymentService;

    private static String payment(final String publicId, final String date) {
        return """
                {"publicId":"%s","targetAccountPublicId":"01JQ5TARGET000000000000007","targetAccountName":"Landlord",\
                "sourceAccountPublicId":"%s","sourceAccountName":"Checking","userPublicId":"%s","username":"tenant",\
                "comment":"Rent","date":"%s","amount":750.25,"created":"2026-02-14T08:00:00",\
                "lastEdit":"2026-02-14T08:00:00"}""".formatted(publicId, ACCOUNT_ID, USER_ID, date);
    }

    private static PaymentDto rent() {
        final var dto = new PaymentDto(ACCOUNT_ID, "01JQ5TARGET000000000000007", new BigDecimal("750.25"),
                LocalDate.of(2026, 2, 14), "Rent", null, null);
        dto.setPublicId(PAYMENT_ID);
        return dto;
    }

    private static Arguments get(final String suffix, final Function<PaymentService, Object> probe,
            final Object expected, final Map<String, String> query) {
        return arguments(suffix, probe, expected, query);
    }

    static Stream<Arguments> readEndpoints() {
        return Stream.of(
                get("/payments", s -> s.getAllPayments(null, null).get(0).getPublicId(), PAYMENT_ID,
                        Map.of("page", "0", "size", "20")),
                get("/payments", s -> s.getAllPayments(3, 1000).get(0).getComment(), "Rent",
                        Map.of("page", "3", "size", "100")),
                get("/payments/user/" + USER_ID, s -> s.getPaymentsByUser(USER_ID).get(0).getUserPublicId(), USER_ID,
                        Map.of()),
                get("/payments/recent", s -> s.getRecentPayments(0).get(0).getUsername(), "tenant",
                        Map.of("limit", "10")),
                get("/payments/recent", s -> s.getRecentPayments(500).get(0).getUsername(), "tenant",
                        Map.of("limit", "100")),
                get("/payments/recent", s -> s.getRecentPayments(5).get(0).getUsername(), "tenant",
                        Map.of("limit", "5")),
                get("/payments/date-range",
                        s -> s.getPaymentsByDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)).get(0)
                                .getDate(),
                        LocalDate.of(2026, 2, 14), Map.of("startDate", "2026-02-01", "endDate", "2026-02-28")),
                get("/payments/source/" + ACCOUNT_ID,
                        s -> s.getPaymentsBySourceAccount(ACCOUNT_ID).get(0).getSourceAccountName(), "Checking",
                        Map.of()),
                get("/payments/target/" + ACCOUNT_ID,
                        s -> s.getPaymentsByTargetAccount(ACCOUNT_ID).get(0).getTargetAccountName(), "Landlord",
                        Map.of()),
                get("/payments/search", s -> s.searchPayments("rent").get(0).getAmount().toPlainString(), "750.25",
                        Map.of("query", "rent")),
                get(PAYMENT_PATH, s -> s.getPaymentById(PAYMENT_ID).orElseThrow().getComment(), "Rent", Map.of()));
    }

    @ParameterizedTest(name = "GET {0} {3}")
    @MethodSource("readEndpoints")
    void readEndpoint_mapsPayload_andSendsComputedQuery(final String suffix,
            final Function<PaymentService, Object> probe, final Object expected, final Map<String, String> query) {
        StubBackend.stub("GET", suffix, 200, PAYMENT_PATH.equals(suffix) ? PAYMENT_JSON : PAYMENTS_JSON);

        final Object actual = probe.apply(this.paymentService);

        assertThat(StubBackend.onlyRequest()).satisfies(request -> {
            assertThat(request.path()).endsWith(suffix);
            assertThat(request.queryParams()).isEqualTo(query);
        });
        assertThat(actual).isEqualTo(expected);
    }

    /** A payment returned both as source and as target appears once: the generated DTO has value equality. */
    @Test
    void getPaymentsByAccount_mergesBothDirectionsNewestFirst_droppingDuplicates() {
        final String shared = payment("SHARED", "2026-02-10");
        StubBackend.stub("GET", "/payments/source/" + ACCOUNT_ID, 200,
                "[" + payment("OLDEST", "2026-02-01") + "," + shared + "]");
        StubBackend.stub("GET", "/payments/target/" + ACCOUNT_ID, 200,
                "[" + payment("NEWEST", "2026-02-20") + "," + shared + "]");

        final var merged = this.paymentService.getPaymentsByAccount(ACCOUNT_ID);

        assertThat(merged).extracting(PaymentResponseDto::getPublicId).containsExactly("NEWEST", "SHARED", "OLDEST");
        assertThat(StubBackend.requests()).extracting(StubBackend.Recorded::path).satisfiesExactly(
                path -> assertThat(path).endsWith("/payments/source/" + ACCOUNT_ID),
                path -> assertThat(path).endsWith("/payments/target/" + ACCOUNT_ID));
    }

    @Test
    void createPayment_returnsCreatedPayment_on201() {
        StubBackend.stub("POST", "/payments", 201, PAYMENT_JSON);

        final var created = this.paymentService.createPayment(rent());

        assertThat(created.getPublicId()).isEqualTo(PAYMENT_ID);
        assertThat(created.getTargetAccountName()).isEqualTo("Landlord");
        final var body = StubBackend.onlyRequest().jsonBody();
        assertThat(body.get("sourceAccountPublicId").asText()).isEqualTo(ACCOUNT_ID);
        assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("750.25");
        assertThat(body.get("date").asText()).isEqualTo("2026-02-14");
    }

    @Test
    void updatePayment_putsToPaymentPath_andReturnsUpdatedPayment_on200() {
        StubBackend.stub("PUT", PAYMENT_PATH, 200, PAYMENT_JSON);

        final var updated = this.paymentService.updatePayment(rent());

        assertThat(updated.getDate()).isEqualTo(LocalDate.of(2026, 2, 14));
        final var request = StubBackend.onlyRequest();
        assertThat(request.path()).endsWith(PAYMENT_PATH);
        assertThat(request.jsonBody().get("comment").asText()).isEqualTo("Rent");
    }

    /** A 422 carries the backend's validation message behind the failure prefix. */
    @ParameterizedTest
    @CsvSource({ "422, Failed to create payment: amount must be positive" })
    void createPayment_failsWithPrefixedMessage(final int status, final String message) {
        StubBackend.stub("POST", "/payments", status, REJECTION_JSON);

        assertThatThrownBy(() -> this.paymentService.createPayment(rent())).isInstanceOf(ServiceException.class)
                .hasMessage(message);
        assertThat(StubBackend.requests()).hasSize(1);
    }

    /** A 422 carries the backend's validation message behind the failure prefix. */
    @ParameterizedTest
    @CsvSource({ "422, Failed to update payment: amount must be positive" })
    void updatePayment_failsWithPrefixedMessage(final int status, final String message) {
        StubBackend.stub("PUT", PAYMENT_PATH, status, REJECTION_JSON);

        assertThatThrownBy(() -> this.paymentService.updatePayment(rent())).isInstanceOf(ServiceException.class)
                .hasMessage(message);
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void deletePayment_succeeds_on204() {
        StubBackend.stub("DELETE", PAYMENT_PATH, 204, null);

        assertThatCode(() -> this.paymentService.deletePayment(PAYMENT_ID)).doesNotThrowAnyException();
        assertThat(StubBackend.onlyRequest().method()).isEqualTo("DELETE");
    }

    /** A 404 means the payment does not exist. */
    @Test
    void getPaymentById_returnsEmpty_on404() {
        StubBackend.stub("GET", PAYMENT_PATH, 404, NOT_FOUND_JSON);

        assertThat(this.paymentService.getPaymentById(PAYMENT_ID)).isEmpty();
        assertThat(StubBackend.requests()).hasSize(1);
    }

    /** deletePayment carries no not-found message, so a 404 surfaces as the generic failure. */
    @Test
    void deletePayment_throwsGenericFailure_on404() {
        StubBackend.stub("DELETE", PAYMENT_PATH, 404, NOT_FOUND_JSON);

        assertThatThrownBy(() -> this.paymentService.deletePayment(PAYMENT_ID)).isInstanceOf(ServiceException.class)
                .hasMessage(GENERIC_FAILURE);
        assertThat(StubBackend.requests()).hasSize(1);
    }

    @Test
    void anyCall_logsOutAndThrows_on401() {
        StubBackend.stub("GET", PAYMENT_PATH, 401, null);

        assertThatThrownBy(() -> this.paymentService.getPaymentById(PAYMENT_ID))
                .isInstanceOf(AuthenticationException.class);

        verify(this.authService).logout();
        assertThat(StubBackend.requests()).hasSize(1);
    }
}
