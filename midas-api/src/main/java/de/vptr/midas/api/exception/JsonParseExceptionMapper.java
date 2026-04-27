package de.vptr.midas.api.exception;

import com.fasterxml.jackson.core.JsonParseException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Maps Jackson JSON parse/mapping errors to 400 Bad Request. */
@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public Response toResponse(final JsonParseException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiErrorResponse(400, "Bad Request", "Malformed JSON request body"))
                .build();
    }
}
