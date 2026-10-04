package de.vptr.midas.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.LaunchMode;

class SchemaManagementGuardTest {

    private static final String STRATEGY = "quarkus.hibernate-orm.schema-management.strategy";

    private static final String LEGACY = "quarkus.hibernate-orm.database.generation";

    private static void init(final LaunchMode launchMode, final Map<String, String> config) {
        new SchemaManagementGuard(launchMode, config).onInit(new Object());
    }

    @ParameterizedTest
    @ValueSource(strings = { "drop-and-create", "drop", "create", "create-drop", "update", "Drop-And-Create" })
    void testProductionRefusesStrategy(final String strategy) {
        final var refusal =
                assertThrows(IllegalStateException.class, () -> init(LaunchMode.NORMAL, Map.of(STRATEGY, strategy)));
        assertTrue(String.valueOf(refusal.getMessage()).contains("QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY"),
                refusal::getMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = { "drop-and-create", "drop", "create", "create-drop", "update" })
    void testProductionRefusesLegacyGeneration(final String strategy) {
        final var refusal = assertThrows(IllegalStateException.class,
                () -> init(LaunchMode.NORMAL, Map.of(STRATEGY, "validate", LEGACY, strategy)));
        assertTrue(String.valueOf(refusal.getMessage()).contains("QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION"),
                refusal::getMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = { "none", "validate", "VALIDATE", " validate ", "None" })
    void testProductionAcceptsStrategy(final String strategy) {
        assertDoesNotThrow(() -> init(LaunchMode.NORMAL, Map.of(STRATEGY, strategy, LEGACY, strategy)));
    }

    @Test
    void testProductionAcceptsAbsentStrategy() {
        assertDoesNotThrow(() -> init(LaunchMode.NORMAL, Map.of()));
    }

    @ParameterizedTest
    @EnumSource(value = LaunchMode.class, names = "NORMAL", mode = EnumSource.Mode.EXCLUDE)
    void testOtherModesAcceptAnyStrategy(final LaunchMode launchMode) {
        assertDoesNotThrow(() -> init(launchMode, Map.of(STRATEGY, "drop-and-create", LEGACY, "drop-and-create")));
    }
}
