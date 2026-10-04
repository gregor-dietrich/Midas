package de.vptr.midas.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class FlywayCleanGuardTest {

    private static FluentConfiguration customize(final LaunchMode launchMode, final List<String> profiles,
            final boolean cleanDisabled) {
        final var configuration = new FluentConfiguration().cleanDisabled(cleanDisabled);
        new FlywayCleanGuard(launchMode, profiles).customize(configuration);
        return configuration;
    }

    @Test
    void testProductionDisablesClean() {
        assertTrue(customize(LaunchMode.NORMAL, List.of("prod"), false).isCleanDisabled());
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void testProductionRefusesNonProductionProfile(final String profile) {
        final var guard = new FlywayCleanGuard(LaunchMode.NORMAL, List.of("prod", profile));
        assertThrows(IllegalStateException.class, () -> guard.onInit(new Object()));
    }

    @ParameterizedTest
    @EnumSource(value = LaunchMode.class, names = "NORMAL", mode = EnumSource.Mode.EXCLUDE)
    void testDevAndTestLaunchesAcceptTheirProfiles(final LaunchMode launchMode) {
        assertDoesNotThrow(() -> new FlywayCleanGuard(launchMode, List.of("dev", "test")).onInit(new Object()));
    }

    /**
     * Reads the profiles the way configuration resolves them, so a parent profile and a profile list count: both make
     * the {@code %dev}/{@code %test} keys apply.
     */
    @ParameterizedTest
    @CsvSource({ "staging, dev", "'prod,test', ''" })
    void testProductionRefusesNonProductionProfileFromConfig(final String profile, final String parent) {
        final var properties = new HashMap<String, String>();
        properties.put(SmallRyeConfig.SMALLRYE_CONFIG_PROFILE, profile);
        if (!parent.isEmpty()) {
            properties.put(SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT, parent);
        }
        final var config = new SmallRyeConfigBuilder().addDefaultInterceptors()
                .withSources(new PropertiesConfigSource(properties, "test", 500)).build();
        final var guard = new FlywayCleanGuard(LaunchMode.NORMAL, config);
        assertThrows(IllegalStateException.class, () -> guard.onInit(new Object()));
    }

    @ParameterizedTest
    @EnumSource(value = LaunchMode.class, names = "NORMAL", mode = EnumSource.Mode.EXCLUDE)
    void testDevAndTestLeaveConfigurationAlone(final LaunchMode launchMode) {
        for (final boolean cleanDisabled : new boolean[] { false, true }) {
            assertEquals(cleanDisabled, customize(launchMode, List.of("dev", "test"), cleanDisabled).isCleanDisabled());
        }
    }
}
