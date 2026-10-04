package de.vptr.midas.gui.component.dialog;

import static de.vptr.midas.gui.util.UiTestUtil.findButton;
import static de.vptr.midas.gui.util.UiTestUtil.findComponent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.textfield.TextArea;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminPostCommentPanelTest extends AbstractDialogTest {

    @Mock
    PostCommentService commentService;

    /** A refused comment shows the backend's reason, as the service carries it from a 422. */
    @Test
    void postComment_showsServiceMessage_whenRefused() {
        final var panel = new AdminPostCommentPanel(this.commentService, this.onAuthFailure);
        final var refusal = new ServiceException("Failed to create comment: Comments are not allowed on this post.");
        when(this.commentService.createComment(any(PostCommentDto.class))).thenThrow(refusal);

        panel.open(new PostResponseDto().publicId("p1").title("News"));
        findComponent(panel, TextArea.class, field -> "Comment".equals(field.getLabel())).setValue("Nice post");
        findButton(panel, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError(refusal,
                "Failed to create comment: Comments are not allowed on this post."));
    }
}
