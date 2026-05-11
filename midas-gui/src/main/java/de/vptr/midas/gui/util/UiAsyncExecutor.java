package de.vptr.midas.gui.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.VaadinSession;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/** Detached-safe async execution for Vaadin views. */
public final class UiAsyncExecutor {

    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private static final Logger LOG = Logger.getLogger(UiAsyncExecutor.class);
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool(new UiThreadFactory());

    private UiAsyncExecutor() {
    }

    private static final class UiThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(final Runnable r) {
            final Thread thread = new Thread(r, "midas-async-" + this.counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * Executes a background task using the default timeout.
     *
     * @param <T>
     *            the result type
     * @param backgroundTask
     *            the supplier performing the background work
     * @param onSuccess
     *            callback invoked with the result on the UI thread
     * @param onError
     *            callback invoked with any throwable on the UI thread
     */
    public static <T> void execute(final Supplier<T> backgroundTask, final Consumer<T> onSuccess,
            final Consumer<Throwable> onError) {
        execute(backgroundTask, onSuccess, onError, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Executes a background task with timeout, session propagation, and detached-safe UI dispatch.
     */
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "Side effect: start async computation")
    public static <T> void execute(final Supplier<T> backgroundTask, final Consumer<T> onSuccess,
            final Consumer<Throwable> onError, final int timeoutSeconds) {
        final UI ui = UI.getCurrent();
        final VaadinSession session = VaadinSession.getCurrent();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final var _ = CompletableFuture.<T>supplyAsync(() -> withSession(session, tccl, backgroundTask), ASYNC_EXECUTOR)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> dispatch(ui, session, () -> {
                    if (throwable != null) {
                        onError.accept(throwable);
                    } else {
                        onSuccess.accept(result);
                    }
                }, throwable));
    }

    /**
     * Executes an async stage supplier using the default timeout.
     *
     * @param <T>
     *            the result type
     * @param stageSupplier
     *            the supplier returning a completion stage
     * @param onSuccess
     *            callback invoked with the result on the UI thread
     * @param onError
     *            callback invoked with any throwable on the UI thread
     */
    public static <T> void executeStage(final Supplier<? extends CompletionStage<T>> stageSupplier,
            final Consumer<T> onSuccess, final Consumer<Throwable> onError) {
        executeStage(stageSupplier, onSuccess, onError, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Executes an async stage supplier with timeout, session propagation, and detached-safe UI dispatch.
     */
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "Side effect: start async computation")
    @SuppressWarnings("FutureReturnValueIgnored")
    public static <T> void executeStage(final Supplier<? extends CompletionStage<T>> stageSupplier,
            final Consumer<T> onSuccess, final Consumer<Throwable> onError, final int timeoutSeconds) {
        final UI ui = UI.getCurrent();
        final VaadinSession session = VaadinSession.getCurrent();
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        final var _ = CompletableFuture
                .supplyAsync(() -> withSession(session, tccl, () -> stageSupplier.get().toCompletableFuture()),
                        ASYNC_EXECUTOR)
                .<T>thenCompose(stage -> stage).orTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((result, throwable) -> dispatch(ui, session, () -> {
                    if (throwable != null) {
                        onError.accept(throwable);
                    } else {
                        onSuccess.accept(result);
                    }
                }, throwable));
    }

    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "Side effect: UI access dispatch")
    static void dispatch(@Nullable final UI ui, @Nullable final VaadinSession session,
            final SerializableRunnable callback, @Nullable final Throwable throwable) {
        if (ui == null || !ui.isAttached()) {
            handleDetachedDispatch(throwable, session);
            return;
        }

        try {
            // access() acquires the VaadinSession lock and enqueues on the UI thread.
            final var _ = ui.access(() -> callback.run());
        } catch (final RuntimeException e) {
            if (throwable != null) {
                MidasErrorHandler.handleDetached(throwable, session);
            } else {
                LOG.debug("Dropped async UI update after detach", e);
            }
        }
    }

    private static <T> T withSession(@Nullable final VaadinSession session, final ClassLoader tccl,
            final Supplier<T> supplier) {
        final VaadinSession previousSession = VaadinSession.getCurrent();
        final ClassLoader previousTccl = Thread.currentThread().getContextClassLoader();
        VaadinSession.setCurrent(session);
        Thread.currentThread().setContextClassLoader(tccl);
        try {
            return supplier.get();
        } finally {
            VaadinSession.setCurrent(previousSession);
            Thread.currentThread().setContextClassLoader(previousTccl);
        }
    }

    private static void handleDetachedDispatch(@Nullable final Throwable throwable,
            @Nullable final VaadinSession session) {
        if (throwable != null) {
            MidasErrorHandler.handleDetached(throwable, session);
        } else {
            LOG.debug("Dropped async UI update after detach");
        }
    }
}
