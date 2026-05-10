package de.vptr.midas.gui.view;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.UiAsyncExecutor;

/** View for base async data. */
public abstract class AbstractAsyncDataView extends VerticalLayout {

    /**
     * Runs dataLoader off the UI thread, then dispatches result to onSuccess on the UI thread. Errors are handled
     * centrally via MidasErrorHandler.
     */
    protected <T> void loadDataAsync(final Supplier<T> dataLoader, final Consumer<T> onSuccess) {
        this.loadDataAsync(dataLoader, onSuccess, UiAsyncExecutor.DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Runs dataLoader off the UI thread with a custom timeout.
     */
    protected <T> void loadDataAsync(final Supplier<T> dataLoader, final Consumer<T> onSuccess,
            final int timeoutSeconds) {
        UiAsyncExecutor.execute(dataLoader, onSuccess, MidasErrorHandler::handleInUiContext, timeoutSeconds);
    }

    /**
     * Runs dataLoader off the UI thread, dispatching result to onSuccess and errors to a custom onError handler — both
     * on the UI thread.
     */
    protected <T> void loadDataAsync(final Supplier<T> dataLoader, final Consumer<T> onSuccess,
            final Consumer<Throwable> onError) {
        this.loadDataAsync(dataLoader, onSuccess, onError, UiAsyncExecutor.DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Full variant with custom error handler and timeout.
     */
    protected <T> void loadDataAsync(final Supplier<T> dataLoader, final Consumer<T> onSuccess,
            final Consumer<Throwable> onError, final int timeoutSeconds) {
        UiAsyncExecutor.execute(dataLoader, onSuccess, onError, timeoutSeconds);
    }
}
