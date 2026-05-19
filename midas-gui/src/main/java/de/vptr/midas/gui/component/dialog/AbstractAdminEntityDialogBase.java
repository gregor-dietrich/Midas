package de.vptr.midas.gui.component.dialog;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.ValueProvider;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;

/** Base class for admin entity dialogs to unify layout, binder handling, and save/cancel flows. */
public abstract class AbstractAdminEntityDialogBase<T> extends FormDialog {

    private final Logger log = Logger.getLogger(this.getClass());

    @Nullable
    protected final Runnable onSave;
    @Nullable
    protected final Runnable onAuthFailure;

    @Nullable
    protected Binder<T> binder;
    @Nullable
    protected transient T currentBean;

    protected AbstractAdminEntityDialogBase(@Nullable final Runnable onSave, @Nullable final Runnable onAuthFailure) {
        super();
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    protected void initDialogLayout(final String titleText, final FormLayout form) {
        final var title = new H3(titleText);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button(this.getSaveButtonLabel(), ignore -> this.handleSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignore -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);
        Objects.requireNonNull(this.binder).readBean(this.currentBean);
        super.open();
    }

    protected String getSaveButtonLabel() {
        return "Save";
    }

    protected abstract void handleSave();

    protected void executeSaveWorkflow(final boolean isNew, final String createMsg, final String updateMsg,
            final Consumer<T> createAction, final Consumer<T> updateAction, final String entityName) {
        try {
            final var b = Objects.requireNonNull(this.binder);
            final var bean = Objects.requireNonNull(this.currentBean);

            if (!b.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            b.writeBean(bean);

            if (!this.preSave(bean)) {
                return;
            }

            if (isNew) {
                createAction.accept(bean);
                NotificationUtil.showSuccess(createMsg);
            } else {
                updateAction.accept(bean);
                NotificationUtil.showSuccess(updateMsg);
            }

            this.close();
            if (this.onSave != null) {
                this.onSave.run();
            }

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            if (this.onAuthFailure != null) {
                this.onAuthFailure.run();
            }
        } catch (final ServiceException e) {
            this.handleServiceException(e, entityName);
        } catch (final RuntimeException e) {
            this.log.error("Unexpected error saving " + entityName.toLowerCase(Locale.ROOT), e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    protected void handleServiceException(final ServiceException e, final String entityName) {
        NotificationUtil.showError(e, "Saving " + entityName.toLowerCase(Locale.ROOT) + " failed. Please try again.");
    }

    protected boolean preSave(final T bean) {
        return true;
    }

    protected FormLayout createResponsiveFormLayout() {
        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return form;
    }

    protected TextField createAndBindRequiredNameField(final ValueProvider<T, String> getter,
            final Setter<T, String> setter) {
        return this.createAndBindRequiredNameField("Name", getter, setter);
    }

    protected TextField createAndBindRequiredNameField(final String label, final ValueProvider<T, String> getter,
            final Setter<T, String> setter) {
        final var nameField = new TextField(label);
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        nameField.setInvalid(false);

        Objects.requireNonNull(this.binder).forField(nameField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), label + " is required")
                .bind(getter, setter);

        return nameField;
    }
}
