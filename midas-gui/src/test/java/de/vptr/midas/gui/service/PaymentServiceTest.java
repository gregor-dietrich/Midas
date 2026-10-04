package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PaymentClient;
import de.vptr.midas.gui.client.PaymentClient.GetPaymentsBySourceAccountRequest;
import de.vptr.midas.gui.client.PaymentClient.GetPaymentsByTargetAccountRequest;
import de.vptr.midas.gui.dto.PaymentResponseDto;

/** Covers the merge in {@link PaymentService#getPaymentsByAccount}; HTTP behaviour is pinned elsewhere. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway")
class PaymentServiceTest {

    @Mock
    @RestClient
    PaymentClient paymentClient;

    @Mock
    AuthService authService;

    @InjectMocks
    PaymentService paymentService;

    /**
     * The merge goes through a {@code HashSet}, whose iteration order follows the hash codes, so varying the undated
     * payment's id and direction hands it to the sort both ahead of and behind the dated ones.
     */
    static Stream<Arguments> undatedPlacements() {
        return IntStream.range(0, 8).mapToObj(i -> arguments("undated-" + i, i % 2 == 0));
    }

    @ParameterizedTest(name = "{0} as source={1}")
    @MethodSource("undatedPlacements")
    void getPaymentsByAccount_sortsPaymentsWithoutDateLast(final String undatedId, final boolean undatedIsSource) {
        final var undated = new PaymentResponseDto().publicId(undatedId);
        final var older = new PaymentResponseDto().publicId("older").date(LocalDate.of(2026, 1, 1));
        final var newer = new PaymentResponseDto().publicId("newer").date(LocalDate.of(2026, 2, 1));
        when(this.paymentClient.getPaymentsBySourceAccount(any(GetPaymentsBySourceAccountRequest.class)))
                .thenReturn(undatedIsSource ? List.of(undated, older) : List.of(older));
        when(this.paymentClient.getPaymentsByTargetAccount(any(GetPaymentsByTargetAccountRequest.class)))
                .thenReturn(undatedIsSource ? List.of(newer) : List.of(newer, undated));

        assertThat(this.paymentService.getPaymentsByAccount("a1")).extracting(PaymentResponseDto::getPublicId)
                .containsExactly("newer", "older", undatedId);
    }
}
