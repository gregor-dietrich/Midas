package de.vptr.midas.api;

import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jboss.logging.Logger;

import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

/**
 * Keeps a production launch away from the dev and test settings, whatever the profile says (#31). {@code %dev,test} set
 * {@code clean-disabled=false} and {@code clean-at-start=true}, both runtime properties, so a production jar started
 * with {@code QUARKUS_PROFILE=dev} would drop the production schema before any startup observer runs, and then serve
 * with the other {@code %dev}/{@code %test} settings (SQL logging, relaxed rate limits).
 *
 * <p>
 * In a production launch the guard therefore refuses a dev or test profile, including one inherited as a parent
 * profile. It does so while the CDI container initialises, before Flyway or Hibernate touch the database. As a backstop
 * it also disables Flyway's clean, for the default datasource. The container-initialised event is delivered this early
 * in JVM mode; a native image may deliver it differently.
 */
@ApplicationScoped
public class FlywayCleanGuard implements FlywayConfigurationCustomizer {

    private static final Logger LOG = Logger.getLogger(FlywayCleanGuard.class);

    private static final List<String> NON_PRODUCTION_PROFILES = List.of("dev", "test");

    /** Ahead of the platform's own container-initialised observers. */
    private static final int BEFORE_PLATFORM = Interceptor.Priority.PLATFORM_BEFORE;

    private final LaunchMode launchMode;

    private final List<String> profiles;

    @Inject
    FlywayCleanGuard(final LaunchMode launchMode, final Config config) {
        this(launchMode, config.unwrap(SmallRyeConfig.class).getProfiles());
    }

    FlywayCleanGuard(final LaunchMode launchMode, final List<String> profiles) {
        this.launchMode = launchMode;
        this.profiles = List.copyOf(profiles);
    }

    void onInit(@Observes @Initialized(ApplicationScoped.class) @Priority(BEFORE_PLATFORM) final Object event) {
        if (this.launchMode != LaunchMode.NORMAL) {
            return;
        }
        final var nonProduction = this.profiles.stream().filter(NON_PRODUCTION_PROFILES::contains).toList();
        if (!nonProduction.isEmpty()) {
            LOG.errorf("FATAL: this production build was started with the %s profile!", nonProduction);
            LOG.error("Unset quarkus.profile (QUARKUS_PROFILE) and quarkus.config.profile.parent, or set the profile "
                    + "to prod, before starting.");
            throw new IllegalStateException("Production build started with a dev or test profile: " + nonProduction);
        }
    }

    @Override
    public void customize(final FluentConfiguration configuration) {
        if (this.launchMode == LaunchMode.NORMAL) {
            configuration.cleanDisabled(true);
        }
    }
}
