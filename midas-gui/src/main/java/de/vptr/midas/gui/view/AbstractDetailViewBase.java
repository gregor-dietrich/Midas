package de.vptr.midas.gui.view;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;

import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;

/** Base view class for single entity detail views using identifiers. */
public abstract class AbstractDetailViewBase<T> extends AbstractAuthenticatedParameterView {

    private final Logger log = Logger.getLogger(this.getClass());

    protected AbstractDetailViewBase() {
        super();
    }

    @Override
    protected void onBeforeEnterAuthenticated(final BeforeEnterEvent event) {
        this.removeAll();

        final var backButton = new Button("← Back to " + this.getEntityPluralName(),
                ignore -> this.getUI().ifPresent(ui -> ui.navigate(this.getBackNavigationTarget())));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        this.add(backButton);

        this.startLoading();
    }

    protected abstract String getEntityPluralName();

    protected abstract String getEntitySingleName();

    protected abstract Class<? extends Component> getBackNavigationTarget();

    private void startLoading() {
        if (this.identifier == null || this.identifier.trim().isEmpty()) {
            NotificationUtil
                    .showError("No " + this.getEntitySingleName().toLowerCase(Locale.ROOT) + " identifier provided");
            return;
        }

        final var idStr = Objects.requireNonNull(this.identifier);
        this.log.infof("Loading %s with identifier: %s", this.getEntitySingleName().toLowerCase(Locale.ROOT), idStr);

        final Long entityId = this.tryParseId(idStr);

        if (entityId != null) {
            this.loadById(entityId);
        } else {
            this.loadByName(idStr);
        }
    }

    @Nullable
    protected Long tryParseId(final String value) {
        try {
            return Long.valueOf(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    protected abstract void loadById(Long id);

    protected abstract void loadByName(String name);

    protected void handleLoadResult(final Optional<T> optionalEntity, final Object searchedId) {
        if (optionalEntity.isPresent()) {
            final var entity = optionalEntity.get();
            this.log.infof("Successfully loaded %s", this.getEntitySingleName().toLowerCase(Locale.ROOT));
            this.renderEntity(entity);
        } else {
            this.log.warnf("%s not found with identifier: %s", this.getEntitySingleName(), searchedId);
            this.renderNotFound();
        }
    }

    protected abstract void renderEntity(T entity);

    protected void renderNotFound() {
        final var notFoundHeader = new H2(this.getEntitySingleName() + " Not Found");
        final var message = new Div();
        message.setText(
                "The requested " + this.getEntitySingleName().toLowerCase(Locale.ROOT) + " could not be found.");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        this.add(notFoundHeader, message);
    }
}
