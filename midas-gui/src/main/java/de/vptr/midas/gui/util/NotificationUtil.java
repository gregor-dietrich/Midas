package de.vptr.midas.gui.util;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/** Utility class for notification. */
public final class NotificationUtil {

    private static final int SHORT_DURATION_MS = 3000;
    private static final int MEDIUM_DURATION_MS = 4000;
    private static final int LONG_DURATION_MS = 5000;

    private NotificationUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Shows a success notification
     * 
     * @param message
     *            The message to display
     */
    public static void showSuccess(final String message) {
        showNotification(message, NotificationVariant.LUMO_SUCCESS, SHORT_DURATION_MS);
    }

    /**
     * Shows an error notification
     * 
     * @param message
     *            The message to display
     */
    public static void showError(final String message) {
        showNotification(message, NotificationVariant.LUMO_ERROR, LONG_DURATION_MS);
    }

    /**
     * Shows a sanitized error notification for an exception.
     *
     * @param throwable
     *            The exception to sanitize and log
     * @param fallbackMessage
     *            The safe fallback message to show for non-auth errors
     */
    public static void showError(final Throwable throwable, final String fallbackMessage) {
        MidasErrorHandler.showSanitizedError(throwable, fallbackMessage);
    }

    /**
     * Shows a warning notification
     * 
     * @param message
     *            The message to display
     */
    public static void showWarning(final String message) {
        showNotification(message, NotificationVariant.LUMO_CONTRAST, MEDIUM_DURATION_MS);
    }

    /**
     * Shows an info notification
     * 
     * @param message
     *            The message to display
     */
    public static void showInfo(final String message) {
        showNotification(message, NotificationVariant.LUMO_PRIMARY, SHORT_DURATION_MS);
    }

    /**
     * Shows a custom notification with specified duration
     * 
     * @param message
     *            The message to display
     * @param variant
     *            The notification variant
     * @param duration
     *            Duration in milliseconds
     */
    public static void showNotification(final String message, final NotificationVariant variant, final int duration) {
        final var notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setText(message);
        notification.setDuration(duration);
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.open();
    }

    /**
     * Shows a notification with custom position
     * 
     * @param message
     *            The message to display
     * @param variant
     *            The notification variant
     * @param duration
     *            Duration in milliseconds
     * @param position
     *            The position of the notification
     */
    public static void showNotification(final String message, final NotificationVariant variant, final int duration,
            final Notification.Position position) {
        final var notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setText(message);
        notification.setDuration(duration);
        notification.setPosition(position);
        notification.open();
    }
}
