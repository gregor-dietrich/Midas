package de.vptr.midas.gui.component.dialog;

import java.util.Objects;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.service.PageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/** Dialog for creating and editing pages in the admin view. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminPageDialog extends AbstractAdminEntityDialogBase<PageDto> {

    private final transient PageService pageService;

    /** Initializes AdminPageDialog. */
    public AdminPageDialog(final PageService pageService, final Runnable onSave, final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.pageService = pageService;
    }

    /** Opens the dialog for creating or editing a page. */
    public void open(@Nullable final PageDto page) {
        this.removeAll();
        this.currentBean = page != null ? page : new PageDto();
        final var b = new Binder<>(PageDto.class);
        this.binder = b;

        final var form = this.createResponsiveFormLayout();
        final var titleField = this.createAndBindRequiredNameField(PageDto::getTitle, PageDto::setTitle);
        titleField.setLabel("Title");

        final var contentField = new TextArea("Content");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("250px");
        contentField.setMaxLength(50_000);
        contentField.setInvalid(false);

        b.forField(contentField).withValidator(value -> value != null && !value.trim().isEmpty(), "Content is required")
                .bind(PageDto::getContent, PageDto::setContent);

        form.add(titleField, contentField);

        this.initDialogLayout(page != null ? "Edit Page" : "Create Page", form);
    }

    @Override
    protected void handleSave() {
        final boolean isNew = Objects.requireNonNull(this.currentBean).getPublicId() == null;
        this.executeSaveWorkflow(isNew, "Page created successfully", "Page updated successfully",
                this.pageService::createPage, this.pageService::updatePage, "Page");
    }
}
