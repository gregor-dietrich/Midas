package de.vptr.midas.api.util;

import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.validation.ValidationException;

/** JPA entity for entity validator data. */
public final class EntityValidator {

    private EntityValidator() {
    }

    /**
     * Validates that no entity exists for the given field and value. Throws ValidationException if a duplicate is
     * found.
     */
    public static <T> void checkUnique(final Optional<T> existing, final String fieldName,
            @Nullable final String value) {
        checkUnique(existing, fieldName, value, "already exists");
    }

    /** Processes the request and returns the result. */
    public static <T> void checkUnique(final Optional<T> existing, final String fieldName, @Nullable final String value,
            final String violationMessage) {
        if (existing.isPresent()) {
            throw new ValidationException(fieldName + " '" + value + "' " + violationMessage);
        }
    }
}
