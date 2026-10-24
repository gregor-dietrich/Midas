package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

class ResponseUtilTest {

    @Test
    void testCreated_withEntity_returns201() {
        final Response response = ResponseUtil.created("new-entity");

        assertEquals(201, response.getStatus());
        assertEquals("new-entity", response.getEntity());
    }

    @Test
    void testCreated_withNull_returns201() {
        final Response response = ResponseUtil.created(null);

        assertEquals(201, response.getStatus());
    }

    @Test
    void testNoContent_returns204() {
        final Response response = ResponseUtil.noContent();

        assertEquals(204, response.getStatus());
    }

    @Test
    void testNotFound_returns404() {
        final Response response = ResponseUtil.notFound();

        assertEquals(404, response.getStatus());
    }

    @Test
    void testOk_noArg_returns200() {
        final Response response = ResponseUtil.ok();

        assertEquals(200, response.getStatus());
    }

    @Test
    void testOk_withEntity_returns200WithEntity() {
        final Response response = ResponseUtil.ok("some-entity");

        assertEquals(200, response.getStatus());
        assertEquals("some-entity", response.getEntity());
    }

    @Test
    void testOk_withNull_returns204() {
        final Response response = ResponseUtil.ok(null);

        assertEquals(204, response.getStatus());
    }

    @Test
    void testOkOrNotFound_presentOptional_returns200() {
        final Response response = ResponseUtil.okOrNotFound(Optional.of("found-entity"));

        assertEquals(200, response.getStatus());
    }

    @Test
    void testOkOrNotFound_emptyOptional_returns404() {
        final Response response = ResponseUtil.okOrNotFound(Optional.empty());

        assertEquals(404, response.getStatus());
    }

    @Test
    void testUnauthorized_returns401() {
        final Response response = ResponseUtil.unauthorized();

        assertEquals(401, response.getStatus());
    }

    @Test
    void testForbidden_returns403() {
        final Response response = ResponseUtil.forbidden();

        assertEquals(403, response.getStatus());
    }

    @Test
    void testUnprocessable_returns422WithEntity() {
        final Response response = ResponseUtil.unprocessable("validation-error");

        assertEquals(422, response.getStatus());
        assertEquals("validation-error", response.getEntity());
    }
}
