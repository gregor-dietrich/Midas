package de.vptr.midas.api.exception;

import de.vptr.midas.api.util.ResponseUtil;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Type definition. */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(final ValidationException exception) {
        final String message = exception != null && exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage() : "Validation failed";
        return ResponseUtil.unprocessable(new ApiErrorResponse(422, "Validation failed", message));
    }
}
