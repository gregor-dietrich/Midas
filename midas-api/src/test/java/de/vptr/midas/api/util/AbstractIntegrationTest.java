package de.vptr.midas.api.util;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/** Base class providing transaction and persistence-context helpers for integration tests. */
public abstract class AbstractIntegrationTest {

    @Inject
    EntityManager entityManager;

    protected void runInIsolatedTransaction(final Runnable action) {
        QuarkusTransaction.requiringNew().run(action);
    }

    protected void runCleanup(final Runnable cleanupAction) {
        try {
            runInIsolatedTransaction(cleanupAction);
        } catch (final RuntimeException exception) {
            clearPersistenceContext();
            throw exception;
        }
    }

    protected void clearPersistenceContext() {
        if (this.entityManager != null && this.entityManager.isOpen()) {
            this.entityManager.clear();
        }
    }
}
