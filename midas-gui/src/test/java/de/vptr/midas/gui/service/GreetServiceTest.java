package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.AuthClient;
import de.vptr.midas.gui.exception.AuthenticationException;
import jakarta.inject.Inject;

@ExtendWith(MockitoExtension.class)
class GreetServiceTest {

    @RestClient
    @Inject
    @Mock
    AuthClient authClient;

    @Mock
    AuthService authService;

    @InjectMocks
    GreetService greetService;

    @Test
    void greet_shouldReturnGreetingWithName_whenAuthenticated() {
        // Given
        final String name = "John";
        when(this.authService.getAuthorizationHeader()).thenReturn("Basic dGVzdDp0ZXN0");

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertThat(result).isEqualTo("Hello, John!");
    }

    @Test
    void greet_shouldReturnGreetingWithAnonymous_whenNameIsNull() {
        // Given
        when(this.authService.getAuthorizationHeader()).thenReturn("Basic dGVzdDp0ZXN0");

        // When
        final String result = this.greetService.greet(null);

        // Then
        assertThat(result).isEqualTo("Hello, anonymous user!");
    }

    @Test
    void greet_shouldReturnGreetingWithAnonymous_whenNameIsEmpty() {
        // Given
        when(this.authService.getAuthorizationHeader()).thenReturn("Basic dGVzdDp0ZXN0");

        // When
        final String result = this.greetService.greet("");

        // Then
        assertThat(result).isEqualTo("Hello, anonymous user!");
    }

    @Test
    void greet_shouldThrowAuthenticationException_whenNotAuthenticated() {
        // Given
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> this.greetService.greet("John"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    void greet_shouldPropagateException_whenAuthServiceThrows() {
        // Given
        final String name = "John";
        when(this.authService.getAuthorizationHeader()).thenThrow(new RuntimeException("Connection error"));

        // When / Then
        assertThatThrownBy(() -> this.greetService.greet(name))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Connection error");
    }
}
