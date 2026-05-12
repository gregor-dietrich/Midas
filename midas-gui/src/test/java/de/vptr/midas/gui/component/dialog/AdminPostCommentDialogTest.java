package de.vptr.midas.gui.component.dialog;

import static de.vptr.midas.gui.util.UiTestUtil.findButton;
import static de.vptr.midas.gui.util.UiTestUtil.findComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.textfield.TextArea;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminPostCommentDialogTest extends AbstractDialogTest {

    @Mock
    PostCommentService commentService;

    @Test
    void saveComment_shouldUpdateComment_whenOpenedWithDto() {
        final var dialog = new AdminPostCommentDialog(this.commentService, this.onSave, this.onAuthFailure);
        final var existingComment = MockDataUtil.createPostCommentDto();
        when(this.commentService.updateComment(any(PostCommentDto.class)))
                .thenReturn(MockDataUtil.createPostCommentDto());

        dialog.open(existingComment);

        findComponent(dialog, TextArea.class, field -> "Content".equals(field.getLabel()))
                .setValue("Updated comment content");

        findButton(dialog, "Save").click();

        final ArgumentCaptor<PostCommentDto> captor = ArgumentCaptor.forClass(PostCommentDto.class);
        verify(this.commentService).updateComment(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Updated comment content");
        assertThat(captor.getValue().getId()).isEqualTo(existingComment.getId());
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Comment updated successfully"));
        verify(this.onSave).run();
    }

    @Test
    void saveComment_shouldCallOnAuthFailure_onAuthException() {
        final var dialog = new AdminPostCommentDialog(this.commentService, this.onSave, this.onAuthFailure);
        final var existingComment = MockDataUtil.createPostCommentDto();
        when(this.commentService.updateComment(any(PostCommentDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        dialog.open(existingComment);

        findComponent(dialog, TextArea.class, field -> "Content".equals(field.getLabel())).setValue("Some content");

        findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }
}
