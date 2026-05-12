package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import de.vptr.midas.gui.util.MidasErrorHandler;

class AbstractAsyncDataViewTest extends AbstractUiTestBase {

    /** Concrete subclass for testing the abstract base */
    private static final class TestAsyncDataView extends AbstractAsyncDataView {
        <T> void testLoadWithErrorHandler(final Supplier<T> loader, final Consumer<T> onSuccess,
                final Consumer<Throwable> onError) {
            this.loadDataAsync(loader, onSuccess, onError);
        }
    }

    @Test
    void loadDataAsync_shouldCallOnSuccess_withCustomErrorHandler() throws Exception {
        final var view = new TestAsyncDataView();
        final var result = new AtomicReference<Integer>();
        final var latch = new CountDownLatch(1);

        view.testLoadWithErrorHandler(() -> 42, value -> {
            result.set(value);
            latch.countDown();
        }, ignore -> {
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(result.get()).isEqualTo(42);
    }

    @Test
    void loadDataAsync_shouldCallCustomErrorHandler_whenLoaderThrows() throws Exception {
        final var view = new TestAsyncDataView();
        final var errorRef = new AtomicReference<Throwable>();
        final var latch = new CountDownLatch(1);

        view.testLoadWithErrorHandler(() -> {
            throw new IllegalStateException("custom error");
        }, ignore -> {
        }, error -> {
            errorRef.set(error);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(errorRef.get()).isNotNull();
    }

    @Test
    void loadDataAsync_shouldReturnCorrectType() throws Exception {
        final var view = new TestAsyncDataView();
        final var result = new AtomicReference<String>();
        final var latch = new CountDownLatch(1);

        view.testLoadWithErrorHandler(() -> "async result", value -> {
            result.set(value);
            latch.countDown();
        }, ignore -> {
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(result.get()).isEqualTo("async result");
    }

    @Test
    void loadDataAsync_shouldTimeoutWithCustomErrorHandler() throws Exception {
        final var view = new TestAsyncDataView();
        final var errorRef = new AtomicReference<Throwable>();
        final var latch = new CountDownLatch(1);

        view.loadDataAsync(() -> {
            try {
                Thread.sleep(1500);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 42;
        }, ignore -> {
        }, error -> {
            errorRef.set(error);
            latch.countDown();
        }, 1);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(MidasErrorHandler.unwrapAsyncThrowable(errorRef.get())).isInstanceOf(TimeoutException.class);
    }
}
