package de.vptr.midas.api;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * Keeps Hibernate from altering the production schema, which Flyway owns (#48). The schema-management strategy is a
 * runtime property, so a production jar started with {@code QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY} set to
 * {@code drop-and-create} drops and recreates every mapped table after Flyway has migrated, deleting all data while the
 * schema history stays intact. The deprecated {@code quarkus.hibernate-orm.database.generation} still has the same
 * effect in Quarkus 3.40.1.
 *
 * <p>
 * In a production launch the guard therefore refuses any strategy other than {@code none} or {@code validate}. It does
 * so while the CDI container initialises, before Hibernate touches the database. The container-initialised event is
 * delivered this early in JVM mode; a native image may deliver it differently. At that point the guard sees the
 * configuration sources available at static init; environment variables, the deployed path, were measured. Only the
 * default persistence unit is checked, as it is the only one.
 */
@ApplicationScoped
public class SchemaManagementGuard {

    private static final Logger LOG = Logger.getLogger(SchemaManagementGuard.class);

    private static final List<String> STRATEGY_PROPERTIES =
            List.of("quarkus.hibernate-orm.schema-management.strategy", "quarkus.hibernate-orm.database.generation");

    private static final Set<String> ALLOWED_STRATEGIES = Set.of("none", "validate");

    /** Ahead of the platform's own container-initialised observers. */
    private static final int BEFORE_PLATFORM = Interceptor.Priority.PLATFORM_BEFORE;

    private final LaunchMode launchMode;

    private final Function<String, Optional<String>> config;

    @Inject
    SchemaManagementGuard(final LaunchMode launchMode, final Config config) {
        this.launchMode = launchMode;
        this.config = name -> config.getOptionalValue(name, String.class);
    }

    SchemaManagementGuard(final LaunchMode launchMode, final Map<String, String> config) {
        this.launchMode = launchMode;
        this.config = name -> Optional.ofNullable(config.get(name));
    }

    void onInit(@Observes @Initialized(ApplicationScoped.class) @Priority(BEFORE_PLATFORM) final Object event) {
        if (this.launchMode != LaunchMode.NORMAL) {
            return;
        }
        for (final String property : STRATEGY_PROPERTIES) {
            final var strategy = this.config.apply(property).orElse("none");
            if (!ALLOWED_STRATEGIES.contains(strategy.strip().toLowerCase(Locale.ROOT))) {
                LOG.errorf("FATAL: %s=%s would let Hibernate alter or drop the production tables!", property, strategy);
                final String envVar = property.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
                LOG.errorf("Unset %s (%s) or set it to validate; Flyway owns the schema.", envVar, property);
                throw new IllegalStateException("Hibernate schema management enabled in production: " + property + "="
                        + strategy + " (" + envVar + ")");
            }
        }
    }
}
