package de.vptr.midas.gui.component.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextArea;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminPostCommentDialogTest {

    @Mock
    PostCommentService commentService;

    private UI ui;
    private Runnable onSave;
    private Runnable onAuthFailure;
    private MockedStatic<NotificationUtil> notificationMock;

    @BeforeEach
    void setUp() {
        this.ui = new UI();
        UI.setCurrent(this.ui);
        this.onSave = mock(Runnable.class);
        this.onAuthFailure = mock(Runnable.class);
        this.notificationMock = mockStatic(NotificationUtil.class);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
        this.notificationMock.close();
    }

    @Test
    void saveComment_shouldUpdateComment_whenOpenedWithDto() {
        final var dialog = new AdminPostCommentDialog(this.commentService, this.onSave, this.onAuthFailure);
        final var existingComment = MockDataUtil.createPostCommentDto();
        when(this.commentService.updateComment(any(PostCommentDto.class)))
                .thenReturn(MockDataUtil.createPostCommentDto());

        dialog.open(existingComment);

        this.findComponent(dialog, TextArea.class, field -> "Content".equals(field.getLabel()))
                .setValue("Updated comment content");

        this.findButton(dialog, "Save").click();

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

        this.findComponent(dialog, TextArea.class, field -> "Content".equals(field.getLabel()))
                .setValue("Some content");

        this.findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }

    private Button findButton(final Component root, final String text) {
        return this.findComponent(root, Button.class, button -> text.equals(button.getText()));
    }

    private <T extends Component> T findComponent(final Component root, final Class<T> type,
            final Predicate<T> predicate) {
        return this.findComponents(root, type).stream().filter(predicate).findFirst()
                .orElseThrow(() -> new AssertionError("Component not found: " + type.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> List<T> findComponents(final Component root, final Class<T> type) {
        final List<T> directMatches =
                root.getChildren().filter(type::isInstance).map(component -> (T) component).toList();
        final List<T> nestedMatches =
                root.getChildren().flatMap(component -> this.findComponents(component, type).stream()).toList();
        return Stream.concat(directMatches.stream(), nestedMatches.stream()).toList();
    }
}
