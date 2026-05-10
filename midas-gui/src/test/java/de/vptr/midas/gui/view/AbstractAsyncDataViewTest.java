package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;

import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AbstractAsyncDataViewTest {

    private UI ui;
    private MockedStatic<UI> uiMock;
    private MockedStatic<NotificationUtil> notificationMock;

    /** Concrete subclass for testing the abstract base */
    private static class TestAsyncDataView extends AbstractAsyncDataView {
        public <T> void testLoadWithErrorHandler(final Supplier<T> loader, final Consumer<T> onSuccess,
                final Consumer<Throwable> onError) {
            this.loadDataAsync(loader, onSuccess, onError);
        }
    }

    @BeforeEach
    void setUp() {
        this.ui = mock(UI.class);
        lenient().when(this.ui.isAttached()).thenReturn(true);
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Command.class).execute();
            return null;
        }).when(this.ui).access(any(Command.class));

        this.uiMock = mockStatic(UI.class);
        this.uiMock.when(UI::getCurrent).thenReturn(this.ui);

        this.notificationMock = mockStatic(NotificationUtil.class);
    }

    @AfterEach
    void tearDown() {
        this.uiMock.close();
        this.notificationMock.close();
    }

    @Test
    void loadDataAsync_shouldCallOnSuccess_withCustomErrorHandler() throws Exception {
        final var view = new TestAsyncDataView();
        final var result = new AtomicReference<Integer>();
        final var latch = new CountDownLatch(1);

        view.testLoadWithErrorHandler(() -> 42, value -> {
            result.set(value);
            latch.countDown();
        }, _ -> {
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
            throw new RuntimeException("custom error");
        }, _ -> {
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
        }, _ -> {
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
        }, _ -> {
        }, error -> {
            errorRef.set(error);
            latch.countDown();
        }, 1);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(MidasErrorHandler.unwrapAsyncThrowable(errorRef.get())).isInstanceOf(TimeoutException.class);
    }
}
