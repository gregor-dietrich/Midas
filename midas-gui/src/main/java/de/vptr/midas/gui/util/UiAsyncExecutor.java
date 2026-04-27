package de.vptr.midas.gui.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.VaadinSession;

/** Detached-safe async execution for Vaadin views. */
public final class UiAsyncExecutor {

    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private static final Logger LOG = Logger.getLogger(UiAsyncExecutor.class);

    private UiAsyncExecutor() {
    }

    public static <T> void execute(final Supplier<T> backgroundTask, final Consumer<T> onSuccess,
            final Consumer<Throwable> onError) {
        execute(backgroundTask, onSuccess, onError, DEFAULT_TIMEOUT_SECONDS);
    }

    /** Executes a background task with timeout, session propagation, and detached-safe UI dispatch. */
    public static <T> void execute(final Supplier<T> backgroundTask, final Consumer<T> onSuccess,
            final Consumer<Throwable> onError, final int timeoutSeconds) {
        final UI ui = UI.getCurrent();
        final VaadinSession session = VaadinSession.getCurrent();

        CompletableFuture.supplyAsync(() -> withSession(session, backgroundTask))
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> dispatch(ui, session, () -> {
                    if (throwable != null) {
                        onError.accept(throwable);
                    } else {
                        onSuccess.accept(result);
                    }
                }, throwable));
    }

    public static <T> void executeStage(final Supplier<? extends CompletionStage<T>> stageSupplier,
            final Consumer<T> onSuccess, final Consumer<Throwable> onError) {
        executeStage(stageSupplier, onSuccess, onError, DEFAULT_TIMEOUT_SECONDS);
    }

    /** Executes an async stage supplier with timeout, session propagation, and detached-safe UI dispatch. */
    public static <T> void executeStage(final Supplier<? extends CompletionStage<T>> stageSupplier,
            final Consumer<T> onSuccess, final Consumer<Throwable> onError, final int timeoutSeconds) {
        final UI ui = UI.getCurrent();
        final VaadinSession session = VaadinSession.getCurrent();

        CompletableFuture.supplyAsync(() -> withSession(session, () -> stageSupplier.get().toCompletableFuture()))
                .thenCompose(stage -> stage)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> dispatch(ui, session, () -> {
                    if (throwable != null) {
                        onError.accept(throwable);
                    } else {
                        onSuccess.accept(result);
                    }
                }, throwable));
    }

    static void dispatch(final UI ui, final VaadinSession session, final SerializableRunnable callback,
            final Throwable throwable) {
        if (ui == null) {
            handleDetachedDispatch(throwable, session);
            return;
        }

        try {
            ui.accessLater(callback, () -> handleDetachedDispatch(throwable, session)).run();
        } catch (final RuntimeException e) {
            if (throwable != null) {
                ErrorHandler.handleDetached(throwable, session);
            } else {
                LOG.debug("Dropped async UI update after detach", e);
            }
        }
    }

    private static <T> T withSession(final VaadinSession session, final Supplier<T> supplier) {
        final VaadinSession previousSession = VaadinSession.getCurrent();
        VaadinSession.setCurrent(session);
        try {
            return supplier.get();
        } finally {
            VaadinSession.setCurrent(previousSession);
        }
    }

    private static void handleDetachedDispatch(final Throwable throwable, final VaadinSession session) {
        if (throwable != null) {
            ErrorHandler.handleDetached(throwable, session);
        } else {
            LOG.debug("Dropped async UI update after detach");
        }
    }
}
