package de.vptr.midas.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/** Application lifecycle management. */
@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = Logger.getLogger(AppLifecycleBean.class);

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "Banner comes from packaged classpath banner.txt and is intentionally multi-line")
    void onStart(@Observes final StartupEvent ev) {
        final String banner = readBanner();
        if (banner != null) {
            LOG.info(banner);
        }
        LOG.info("Welcome to Midas GUI! o/");
    }

    void onStop(@Observes final ShutdownEvent ev) {
        LOG.info("Midas GUI is shutting down. Goodbye! \\o");
    }

    @Nullable
    private String readBanner() {
        try (InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("banner.txt")) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOG.warn("Failed to read banner.txt", e);
            return null;
        }
    }
}
