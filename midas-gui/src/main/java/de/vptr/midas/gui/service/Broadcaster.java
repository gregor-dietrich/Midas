package de.vptr.midas.gui.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.vaadin.flow.shared.Registration;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A simple broadcaster to push updates to multiple UIs.
 */
@ApplicationScoped
public class Broadcaster implements Serializable {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private static final List<Consumer<String>> LISTENERS = new ArrayList<>();

    /**
     * Registers a listener.
     *
     * @param listener
     *            the listener
     * @return a registration to unregister
     */
    public synchronized Registration register(final Consumer<String> listener) {
        LISTENERS.add(listener);
        return () -> {
            synchronized (Broadcaster.class) {
                LISTENERS.remove(listener);
            }
        };
    }

    /**
     * Broadcasts a message to all listeners.
     *
     * @param message
     *            the message
     */
    public synchronized void broadcast(final String message) {
        for (final var listener : LISTENERS) {
            EXECUTOR.execute(() -> listener.accept(message));
        }
    }
}
