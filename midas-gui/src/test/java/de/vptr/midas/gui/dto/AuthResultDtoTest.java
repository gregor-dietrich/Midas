package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthResultDtoTest {

    @Test
    void success_shouldCreateSuccessResult() {
        // When
        final AuthResultDto result = AuthResultDto.success();

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.SUCCESS);
        assertThat(result.getMessage()).isEqualTo("Authentication successful");
    }

    @Test
    void invalidCredentials_shouldCreateInvalidCredentialsResult() {
        // When
        final AuthResultDto result = AuthResultDto.invalidCredentials();

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.INVALID_CREDENTIALS);
        assertThat(result.getMessage()).isEqualTo("Invalid username or password");
    }

    @Test
    void invalidInput_shouldCreateInvalidInputResult() {
        // When
        final AuthResultDto result = AuthResultDto.invalidInput();

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.INVALID_INPUT);
        assertThat(result.getMessage()).isEqualTo("Username and password are required");
    }

    @Test
    void backendUnavailable_shouldCreateBackendUnavailableResult() {
        // When
        final AuthResultDto result = AuthResultDto.backendUnavailable();

        // Then
        assertThat(result.getStatus()).isEqualTo(AuthResultDto.Status.BACKEND_UNAVAILABLE);
        assertThat(result.getMessage()).isEqualTo("Backend service unavailable. Please try again later.");
    }
}
