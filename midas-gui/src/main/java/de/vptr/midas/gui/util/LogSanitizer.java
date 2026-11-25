package de.vptr.midas.gui.util;

import jakarta.annotation.Nullable;

/** Sanitizes values before logging them so CR/LF characters cannot forge log lines. */
public final class LogSanitizer {

    private LogSanitizer() {
        // utility class
    }

    /** Replaces carriage returns and line feeds in {@code value}'s string form with escaped sequences. */
    public static String sanitize(@Nullable final Object value) {
        if (value == null) {
            return "null";
        }
        return value.toString().replace("\r", "\\r").replace("\n", "\\n");
    }
}
