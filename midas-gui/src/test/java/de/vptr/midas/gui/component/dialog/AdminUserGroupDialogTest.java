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
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminUserGroupDialogTest {

        @Mock
        UserGroupService groupService;

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
        void saveGroup_shouldCreateGroup_withValidFields() {
                final var dialog = new AdminUserGroupDialog(this.groupService, this.onSave, this.onAuthFailure);
                when(this.groupService.createGroup(any(UserGroupDto.class)))
                                .thenReturn(MockDataUtil.createUserGroupDto());

                dialog.open(null);

                this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel()))
                                .setValue("Administrators");

                this.findButton(dialog, "Save").click();

                final ArgumentCaptor<UserGroupDto> captor = ArgumentCaptor.forClass(UserGroupDto.class);
                verify(this.groupService).createGroup(captor.capture());
                assertThat(captor.getValue().getName()).isEqualTo("Administrators");
                this.notificationMock.verify(() -> NotificationUtil.showSuccess("Group created successfully"));
                verify(this.onSave).run();
        }

        @Test
        void saveGroup_shouldCallOnAuthFailure_onAuthException() {
                final var dialog = new AdminUserGroupDialog(this.groupService, this.onSave, this.onAuthFailure);
                when(this.groupService.createGroup(any(UserGroupDto.class)))
                                .thenThrow(new AuthenticationException("Session expired"));

                dialog.open(null);

                this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel()))
                                .setValue("Failing Group");

                this.findButton(dialog, "Save").click();

                this.notificationMock
                                .verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
                verify(this.onAuthFailure).run();
        }

        private Button findButton(final Component root, final String text) {
                return this.findComponent(root, Button.class, button -> text.equals(button.getText()));
        }

        private <T extends Component> T findComponent(final Component root, final Class<T> type,
                        final Predicate<T> predicate) {
                return this.findComponents(root, type).stream()
                                .filter(predicate)
                                .findFirst()
                                .orElseThrow(() -> new AssertionError("Component not found: " + type.getSimpleName()));
        }

        @SuppressWarnings("unchecked")
        private <T extends Component> List<T> findComponents(final Component root, final Class<T> type) {
                final List<T> directMatches = root.getChildren()
                                .filter(type::isInstance)
                                .map(component -> (T) component)
                                .toList();
                final List<T> nestedMatches = root.getChildren()
                                .flatMap(component -> this.findComponents(component, type).stream())
                                .toList();
                return Stream.concat(directMatches.stream(), nestedMatches.stream()).toList();
        }
}
