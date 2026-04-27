package de.vptr.midas.gui.service;

import java.util.Optional;

import org.jboss.logging.Logger;

import com.vaadin.flow.server.VaadinSession;

final class SessionAttribute<T> {

    private final String name;
    private final Class<T> type;

    private SessionAttribute(final String name, final Class<T> type) {
        this.name = name;
        this.type = type;
    }

    static <T> SessionAttribute<T> of(final String name, final Class<T> type) {
        return new SessionAttribute<>(name, type);
    }

    Optional<T> read(final VaadinSession session, final Logger logger) {
        if (session == null) {
            return Optional.empty();
        }

        final Object value = session.getAttribute(this.name);
        if (value == null) {
            return Optional.empty();
        }
        if (!this.type.isInstance(value)) {
            logger.warnf("Ignoring unexpected value type for session attribute %s", this.name);
            session.setAttribute(this.name, null);
            return Optional.empty();
        }
        return Optional.of(this.type.cast(value));
    }

    void write(final VaadinSession session, final T value) {
        if (session != null) {
            session.setAttribute(this.name, value);
        }
    }

    void clear(final VaadinSession session) {
        if (session != null) {
            session.setAttribute(this.name, null);
        }
    }
}
