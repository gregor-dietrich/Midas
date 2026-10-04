package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.client.PostCommentClient;
import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.MockDataUtil;
import jakarta.enterprise.event.Event;
import jakarta.ws.rs.WebApplicationException;

/** Covers the comment-created event; HTTP behaviour is pinned by {@link PostCommentServiceHttpContractTest}. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway")
class PostCommentServiceTest {

    @Mock
    @RestClient
    PostCommentClient commentClient;

    @Mock
    AuthService authService;

    @Mock
    Event<String> commentEvent;

    @InjectMocks
    PostCommentService commentService;

    @Test
    void createComment_announcesTheCommentedPost() {
        when(this.commentClient.createComment(any(PostCommentDto.class)))
                .thenReturn(new PostCommentResponseDto().publicId("c1").postPublicId("p1"));

        final var created = this.commentService.createComment(MockDataUtil.createPostCommentDto());

        assertThat(created.getPublicId()).isEqualTo("c1");
        verify(this.commentEvent).fire("comment-created:p1");
    }

    @Test
    void createComment_announcesNothing_whenRejected() {
        when(this.commentClient.createComment(any(PostCommentDto.class))).thenThrow(new WebApplicationException(500));

        assertThatThrownBy(() -> this.commentService.createComment(new PostCommentDto()))
                .isInstanceOf(ServiceException.class);

        verifyNoInteractions(this.commentEvent);
    }
}
