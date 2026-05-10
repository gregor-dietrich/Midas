package de.vptr.midas.gui.dto;

/** Data transfer object for auth result. */
public class AuthResultDto {

    private static final String BACKEND_UNAVAILABLE_MESSAGE = "Backend service unavailable. Please try again later.";

    /** Status component. */
    public enum Status {
        SUCCESS, INVALID_CREDENTIALS, BACKEND_UNAVAILABLE, INVALID_INPUT
    }

    private final Status status;
    private final String message;

    private AuthResultDto(final Status status, final String message) {
        this.status = status;
        this.message = message;
    }

    public static AuthResultDto success() {
        return new AuthResultDto(Status.SUCCESS, "Authentication successful");
    }

    public static AuthResultDto invalidCredentials() {
        return new AuthResultDto(Status.INVALID_CREDENTIALS, "Invalid username or password");
    }

    public static AuthResultDto backendUnavailable() {
        return new AuthResultDto(Status.BACKEND_UNAVAILABLE, BACKEND_UNAVAILABLE_MESSAGE);
    }

    public static AuthResultDto invalidInput() {
        return new AuthResultDto(Status.INVALID_INPUT, "Username and password are required");
    }

    public Status getStatus() {
        return this.status;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean isSuccess() {
        return this.status == Status.SUCCESS;
    }
}
