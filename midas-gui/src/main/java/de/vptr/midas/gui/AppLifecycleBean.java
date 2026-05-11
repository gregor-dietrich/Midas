package de.vptr.midas.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/** Application lifecycle management. */
@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = Logger.getLogger(AppLifecycleBean.class);

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
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("banner.txt")) {
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
