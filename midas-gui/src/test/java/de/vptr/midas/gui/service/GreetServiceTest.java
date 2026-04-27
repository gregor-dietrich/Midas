package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.AuthClient;
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
    void greet_shouldReturnErrorMessage_whenNotAuthenticated() {
        // Given
        when(this.authService.getAuthorizationHeader()).thenReturn(null);

        // When
        final String result = this.greetService.greet("John");

        // Then
        assertThat(result).isEqualTo("Error: Not authenticated");
    }

    @Test
    void greet_shouldReturnSessionExpiredMessage_when401Error() {
        // Given
        final String name = "John";
        when(this.authService.getAuthorizationHeader()).thenThrow(new RuntimeException("HTTP 401"));

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertThat(result).isEqualTo("Error: Session expired. Please log in again.");
        verify(this.authService).logout();
    }

    @Test
    void greet_shouldReturnBackendErrorMessage_whenOtherException() {
        // Given
        final String name = "John";
        when(this.authService.getAuthorizationHeader()).thenThrow(new RuntimeException("Connection error"));

        // When
        final String result = this.greetService.greet(name);

        // Then
        assertThat(result).isEqualTo("Error: Could not connect to backend service");
    }
}
