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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@SuppressWarnings("NullAway")
@ExtendWith(MockitoExtension.class)
class AdminPostCategoryDialogTest {

    @Mock
    PostCategoryService categoryService;

    private UI ui;
    private Runnable onSave;
    private Runnable onAuthFailure;
    private MockedStatic<NotificationUtil> notificationMock;

    // CPD-OFF
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
    // CPD-ON

    @Test
    void saveCategory_shouldCreateCategory_withNameAndParent() {
        final var dialog = new AdminPostCategoryDialog(this.categoryService, this.onSave, this.onAuthFailure);
        final var parentCategory = MockDataUtil.createPostCategoryResponseDto(10L, "Parent Category", null, 0, 5, true);
        final var allCategories = List.of(parentCategory);
        when(this.categoryService.createCategory(any(PostCategoryDto.class)))
                .thenReturn(MockDataUtil.createPostCategoryDto());

        dialog.open(null, allCategories);

        this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("New Category");
        this.findParentComboBox(dialog).setValue(parentCategory.toPostCategoryDto());

        this.findButton(dialog, "Save").click();

        final ArgumentCaptor<PostCategoryDto> captor = ArgumentCaptor.forClass(PostCategoryDto.class);
        verify(this.categoryService).createCategory(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Category");
        assertThat(captor.getValue().getParent()).isNotNull();
        assertThat(captor.getValue().getParent().getId()).isEqualTo(10L);
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Category created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void saveCategory_shouldCallOnAuthFailure_onAuthException() {
        final var dialog = new AdminPostCategoryDialog(this.categoryService, this.onSave, this.onAuthFailure);
        when(this.categoryService.createCategory(any(PostCategoryDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        dialog.open(null, List.of());

        this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel()))
                .setValue("Failing Category");

        this.findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }

    // CPD-OFF
    private Button findButton(final Component root, final String text) {
        return this.findComponent(root, Button.class, button -> text.equals(button.getText()));
    }

    @SuppressWarnings("unchecked")
    private ComboBox<PostCategoryDto> findParentComboBox(final Component root) {
        return (ComboBox<PostCategoryDto>) this.findComponent(root, ComboBox.class,
                field -> "Parent Category".equals(field.getLabel()));
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
    // CPD-ON
}
