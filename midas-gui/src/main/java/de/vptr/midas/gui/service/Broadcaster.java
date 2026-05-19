package de.vptr.midas.gui.service;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();

    /**
     * Registers a listener.
     *
     * @param listener
     *            the listener
     * @return a registration to unregister
     */
    public Registration register(final Consumer<String> listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    /**
     * Broadcasts a message to all listeners.
     *
     * @param message
     *            the message
     */
    public void broadcast(final String message) {
        for (final var listener : LISTENERS) {
            EXECUTOR.execute(() -> listener.accept(message));
        }
    }
}
