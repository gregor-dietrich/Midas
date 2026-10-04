package de.vptr.midas.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Pins that Quarkus discovers {@link FlywayCleanGuard}. A test launch cannot exercise its production branch; that was
 * checked end to end on a production jar (#31).
 */
@QuarkusTest
class FlywayCleanGuardWiringTest {

    /** Default-qualified, as Flyway looks up the customizers of the default datasource. */
    @Inject
    Instance<FlywayConfigurationCustomizer> customizers;

    @Test
    void testGuardIsRegisteredAsFlywayCustomizer() {
        assertTrue(this.customizers.stream().anyMatch(FlywayCleanGuard.class::isInstance));
    }
}
