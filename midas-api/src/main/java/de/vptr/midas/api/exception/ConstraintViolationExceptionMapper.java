package de.vptr.midas.api.exception;

import de.vptr.midas.api.util.ResponseUtil;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Type definition. */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        final var message = exception.getConstraintViolations().stream().map(violation -> violation.getMessage())
                .reduce((a, b) -> a + ", " + b).orElse("Validation failed");

        return ResponseUtil.unprocessable(new ApiErrorResponse(422, "Validation failed", message));
    }
}
