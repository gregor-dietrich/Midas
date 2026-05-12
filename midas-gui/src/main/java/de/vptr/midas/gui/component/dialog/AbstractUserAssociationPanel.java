package de.vptr.midas.gui.component.dialog;

import java.util.Objects;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;

import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;

/** Base dialog panel for managing user associations (e.g. account users, group members). */
@SuppressWarnings("NullAway")
public abstract class AbstractUserAssociationPanel<T, U> extends FormDialog {

    private final Logger log = Logger.getLogger(this.getClass());
    protected final Runnable onAuthFailure;

    @Nullable
    protected Grid<U> userGrid;
    @Nullable
    protected ComboBox<UserResponseDto> availableUsersCombo;
    @Nullable
    protected transient T selectedEntity;

    protected AbstractUserAssociationPanel(final Runnable onAuthFailure) {
        super();
        this.onAuthFailure = onAuthFailure;
    }

    protected void initLayout(final String titleText) {
        this.removeAll();

        final var title = new H3(titleText);

        this.userGrid = this.createGrid();
        this.userGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.userGrid.setHeight("300px");

        this.configureGridColumns(this.userGrid);

        final var addUserLayout = new HorizontalLayout();
        addUserLayout.setAlignItems(VerticalLayout.Alignment.END);
        addUserLayout.setSpacing(true);

        this.availableUsersCombo = new ComboBox<>("Add User");
        this.availableUsersCombo.setItemLabelGenerator(
                user -> user.getUsername() + " (" + (user.getEmail() != null ? user.getEmail() : "no email") + ")");
        this.availableUsersCombo.setWidth("300px");
        this.availableUsersCombo.setPlaceholder("Select a user to add...");

        final var addButton = new Button("Add User", ignore -> this.addUser());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addUserLayout.add(this.availableUsersCombo, addButton);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(ignore -> this.loadAssociatedUsers());
        final var closeButton = new Button("Close", ignore -> this.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(refreshButton, closeButton);

        final var dialogLayout = new VerticalLayout(title, this.userGrid, addUserLayout, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);

        this.loadAssociatedUsers();
        this.loadAvailableUsers();

        super.open();
    }

    protected abstract Grid<U> createGrid();

    protected abstract void configureGridColumns(Grid<U> grid);

    protected abstract void loadAssociatedUsers();

    protected abstract void loadAvailableUsers();

    protected abstract void addUser();

    protected abstract Long getUserId(U item);

    protected boolean isUserAssociated(final Long userId) {
        return this.userGrid != null && this.userGrid.getDataProvider().fetch(new Query<>())
                .anyMatch(item -> Objects.equals(this.getUserId(item), userId));
    }

    /**
     * Validates the current combo selection: warns and returns empty if nothing is selected or if the user is already
     * associated. Subclasses call this at the start of {@link #addUser()}.
     *
     * @param alreadyAssociatedMessage
     *            warning shown when the selected user is already present
     * @return the selected user, or empty when the caller should abort
     */
    protected Optional<UserResponseDto> acquireSelectedUser(final String alreadyAssociatedMessage) {
        final var combo = Objects.requireNonNull(this.availableUsersCombo);
        final var selectedUser = combo.getValue();
        if (selectedUser == null) {
            NotificationUtil.showWarning("Please select a user to add");
            return Optional.empty();
        }
        if (this.isUserAssociated(selectedUser.getId())) {
            NotificationUtil.showWarning(alreadyAssociatedMessage);
            combo.clear();
            return Optional.empty();
        }
        return Optional.of(selectedUser);
    }

    protected void executeWithHandling(final Runnable action, final String errorMessage) {
        try {
            action.run();
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, errorMessage);
        } catch (final RuntimeException e) {
            this.log.error("Unexpected error: " + errorMessage, e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
