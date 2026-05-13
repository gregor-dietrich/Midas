package de.vptr.midas.gui.component.dialog;

import static de.vptr.midas.gui.util.UiTestUtil.findButton;
import static de.vptr.midas.gui.util.UiTestUtil.findComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@SuppressWarnings("NullAway")
@ExtendWith(MockitoExtension.class)
class AdminPostCategoryDialogTest extends AbstractDialogTest {

    @Mock
    PostCategoryService categoryService;

    @Test
    void saveCategory_shouldCreateCategory_withNameAndParent() {
        final var dialog = new AdminPostCategoryDialog(this.categoryService, this.onSave, this.onAuthFailure);
        final var parentCategory =
                MockDataUtil.createPostCategoryResponseDto("10", "Parent Category", null, 0, 5, true);
        final var allCategories = List.of(parentCategory);
        when(this.categoryService.createCategory(any(PostCategoryDto.class)))
                .thenReturn(MockDataUtil.createPostCategoryDto());

        dialog.open(null, allCategories);

        findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("New Category");
        this.findParentComboBox(dialog).setValue(parentCategory.toPostCategoryDto());

        findButton(dialog, "Save").click();

        final ArgumentCaptor<PostCategoryDto> captor = ArgumentCaptor.forClass(PostCategoryDto.class);
        verify(this.categoryService).createCategory(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Category");
        assertThat(captor.getValue().getParent()).isNotNull();
        assertThat(captor.getValue().getParent().getPublicId()).isEqualTo("10");
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Category created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void saveCategory_shouldCallOnAuthFailure_onAuthException() {
        final var dialog = new AdminPostCategoryDialog(this.categoryService, this.onSave, this.onAuthFailure);
        when(this.categoryService.createCategory(any(PostCategoryDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        dialog.open(null, List.of());

        findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("Failing Category");

        findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<PostCategoryDto> findParentComboBox(final Component root) {
        return (ComboBox<PostCategoryDto>) findComponent(root, ComboBox.class,
                field -> "Parent Category".equals(field.getLabel()));
    }

}
