package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BroadcasterTest {

    private Broadcaster broadcaster;

    @BeforeEach
    void setUp() {
        this.broadcaster = new Broadcaster();
    }

    @Test
    void register_shouldAddListener() {
        final List<String> received = new ArrayList<>();
        this.broadcaster.register(received::add);

        this.broadcaster.broadcast("test");

        // Wait a bit for the executor
        this.waitForBroadcast();

        assertThat(received).containsExactly("test");
    }

    @Test
    void unregister_shouldRemoveListener() {
        final List<String> received = new ArrayList<>();
        final var registration = this.broadcaster.register(received::add);
        registration.remove();

        this.broadcaster.broadcast("test");
        this.waitForBroadcast();

        assertThat(received).isEmpty();
    }

    @Test
    void broadcast_shouldNotifyMultipleListeners() {
        final List<String> received1 = new ArrayList<>();
        final List<String> received2 = new ArrayList<>();
        this.broadcaster.register(received1::add);
        this.broadcaster.register(received2::add);

        this.broadcaster.broadcast("hello");
        this.waitForBroadcast();

        assertThat(received1).containsExactly("hello");
        assertThat(received2).containsExactly("hello");
    }

    private void waitForBroadcast() {
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
