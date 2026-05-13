package de.vptr.midas.gui.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Bridges CDI events to the Broadcaster for real-time UI updates.
 */
@ApplicationScoped
public class CommentEventObserver {

    @Inject
    Broadcaster broadcaster;

    /**
     * Observes comment-created events and broadcasts them.
     *
     * @param message
     *            the event message
     */
    public void onCommentCreated(@Observes final String message) {
        if (message != null && message.startsWith("comment-created:")) {
            this.broadcaster.broadcast(message);
        }
    }
}
