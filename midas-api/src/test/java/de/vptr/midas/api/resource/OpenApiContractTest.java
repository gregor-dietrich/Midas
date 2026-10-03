package de.vptr.midas.api.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Guards the committed OpenAPI contract against silent drift. The published API surface is a contract the GUI codes
 * against, so a change to it must be an explicit, reviewable diff rather than a side effect of editing a resource.
 */
@QuarkusTest
class OpenApiContractTest {

    private static final Path SNAPSHOT = Path.of("openapi.yaml");

    private static final String DRIFT_HINT = "The generated OpenAPI contract no longer matches " + SNAPSHOT
            + ". If the API change was intentional, run `make openapi` and commit the updated snapshot.";

    @Test
    void generatedContractMatchesCommittedSnapshot() throws Exception {
        assertTrue(Files.isRegularFile(SNAPSHOT), "Missing committed OpenAPI snapshot: " + SNAPSHOT);

        final String generated =
                TestUtil.given().when().get("/q/openapi?format=json").then().statusCode(200).extract().asString();

        final JsonNode actual = new ObjectMapper().readTree(generated);
        final JsonNode expected = new ObjectMapper(new YAMLFactory()).readTree(Files.readString(SNAPSHOT));

        assertEquals(canonicalize(expected), canonicalize(actual), DRIFT_HINT);
    }

    /**
     * Renders a document as pretty-printed JSON with map keys sorted, so an assertion failure reads as a line-oriented
     * diff instead of a single unreadable blob.
     */
    private static String canonicalize(final JsonNode document) throws Exception {
        if (document instanceof final ObjectNode node) {
            // The server list is derived from the runtime host and port, which differ between the
            // build-time export and the port @QuarkusTest binds to.
            node.remove("servers");
        }
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.treeToValue(document, Object.class));
    }
}
