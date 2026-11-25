package de.vptr.midas.api.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonProcessingException;

/** Maps Jackson JSON parse/mapping errors to 400 Bad Request. */
@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(final JsonProcessingException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiErrorResponse(400, "Bad Request", "Malformed JSON request body")).build();
    }
}
