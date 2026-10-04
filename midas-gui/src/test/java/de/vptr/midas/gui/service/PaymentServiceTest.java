package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PaymentClient;
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

    @Test
    void getPaymentsByAccount_toleratesPaymentsWithoutDate() {
        final var undated = new PaymentResponseDto().publicId("undated");
        final var dated = new PaymentResponseDto().publicId("dated").date(LocalDate.of(2026, 1, 1));
        when(this.paymentClient.getPaymentsBySourceAccount("a1")).thenReturn(List.of(undated));
        when(this.paymentClient.getPaymentsByTargetAccount("a1")).thenReturn(List.of(dated));

        assertThat(this.paymentService.getPaymentsByAccount("a1")).extracting(PaymentResponseDto::getPublicId)
                .containsExactlyInAnyOrder("undated", "dated");
    }
}
