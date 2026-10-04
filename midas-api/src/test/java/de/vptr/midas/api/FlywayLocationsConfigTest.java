package de.vptr.midas.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Production must not load the demo accounts in db/demo. Reads application.properties alone, with no environment or
 * system property source, so the result depends only on the committed profile prefixes.
 */
class FlywayLocationsConfigTest {

    private static final String LOCATIONS = "quarkus.flyway.locations";

    private static PropertiesConfigSource source() throws IOException {
        return new PropertiesConfigSource(
                Objects.requireNonNull(FlywayLocationsConfigTest.class.getResource("/application.properties")));
    }

    private static List<String> locations(final String profile) throws IOException {
        final var config =
                new SmallRyeConfigBuilder().addDefaultInterceptors().withProfile(profile).withSources(source()).build();
        return config.getOptionalValues(LOCATIONS, String.class).orElse(List.of());
    }

    /**
     * Production leaves the locations unset, i.e. Quarkus's default db/migration. Asserting unset rather than "no
     * db/demo" also catches a parent location such as db, which Flyway would scan recursively.
     */
    @Test
    void testProdUsesDefaultLocations() throws IOException {
        assertEquals(List.of(), locations("prod"), () -> "prod " + LOCATIONS + " is set");
    }

    /** The builder above resolves profile prefixes but not Quarkus's profile keys, so the file must not set them. */
    @Test
    void testFileSetsNoProfile() throws IOException {
        assertTrue(source().getPropertyNames().stream()
                .noneMatch(name -> name.endsWith("quarkus.profile") || name.endsWith("quarkus.config.profile.parent")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void testDevAndTestIncludeDemoData(final String profile) throws IOException {
        assertEquals(List.of("db/migration", "db/demo"), locations(profile));
    }
}
