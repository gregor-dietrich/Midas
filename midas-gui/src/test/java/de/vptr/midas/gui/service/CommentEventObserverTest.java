package de.vptr.midas.gui.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentEventObserverTest {

    @Mock
    Broadcaster broadcaster;

    @InjectMocks
    CommentEventObserver observer;

    @Test
    void onCommentCreated_shouldBroadcast_whenValidMessage() {
        final String message = "comment-created:123";
        this.observer.onCommentCreated(message);
        verify(this.broadcaster).broadcast(message);
    }

    @Test
    void onCommentCreated_shouldNotBroadcast_whenInvalidMessage() {
        this.observer.onCommentCreated("invalid");
        verifyNoInteractions(this.broadcaster);
    }

    @Test
    @SuppressWarnings("NullAway")
    void onCommentCreated_shouldNotBroadcast_whenNullMessage() {
        this.observer.onCommentCreated(null);
        verifyNoInteractions(this.broadcaster);
    }
}
