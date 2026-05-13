package de.vptr.midas.gui.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.vptr.midas.gui.client.AuthClient;
import de.vptr.midas.gui.client.BackendHealthClient;
import io.quarkus.runtime.StartupEvent;

@SuppressWarnings("microprofile-restclient")
class RestClientInitializerTest {

    @Mock
    BackendHealthClient backendHealthClient;

    @Mock
    AuthClient authClient;

    @InjectMocks
    RestClientInitializer initializer;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        this.closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        this.closeable.close();
    }

    @Test
    void onStartup_shouldSkipInTestMode() {
        // Test mode is detected via system property "maven.surefire.plugin.version"
        // In this test, it should be set if run via maven, but let's ensure it for the test
        final String previousValue = System.getProperty("maven.surefire.plugin.version");
        System.setProperty("maven.surefire.plugin.version", "test");
        try {
            this.initializer.onStartup(mock(StartupEvent.class));
            verifyNoInteractions(this.backendHealthClient, this.authClient);
        } finally {
            if (previousValue != null) {
                System.setProperty("maven.surefire.plugin.version", previousValue);
            } else {
                System.clearProperty("maven.surefire.plugin.version");
            }
        }
    }

    @Test
    void onStartup_shouldInitialize_whenNotTestMode() {
        // Manually clear the property to simulate non-test mode (though it's risky in real tests)
        final String previousValue = System.getProperty("maven.surefire.plugin.version");
        System.clearProperty("maven.surefire.plugin.version");
        try {
            // We expect it to try and touch the clients
            this.initializer.onStartup(mock(StartupEvent.class));
            // It calls checkHealth() which we should verify
            verify(this.backendHealthClient).checkHealth();
        } finally {
            if (previousValue != null) {
                System.setProperty("maven.surefire.plugin.version", previousValue);
            }
        }
    }
}
