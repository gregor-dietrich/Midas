package de.vptr.midas.api.util;

import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * Utility class for monitoring and logging performance metrics
 */
public class PerformanceMonitor {

    private static final Logger LOG = Logger.getLogger(PerformanceMonitor.class);
    private static final long SLOW_THRESHOLD_MS = 1000;
    private static final long INFO_THRESHOLD_MS = 100;

    /**
     * Executes a supplier and logs the execution time
     * 
     * @param operation
     *            Description of the operation being timed
     * @param supplier
     *            The operation to execute
     * @return The result of the supplier
     */
    public static <T> T time(final String operation, final Supplier<T> supplier) {
        final long startTime = System.currentTimeMillis();
        try {
            final T result = supplier.get();
            final long endTime = System.currentTimeMillis();
            final long duration = endTime - startTime;

            if (duration > SLOW_THRESHOLD_MS) {
                LOG.warnf("SLOW QUERY: %s took %sms", operation, duration);
            } else if (duration > INFO_THRESHOLD_MS) {
                LOG.infof("PERFORMANCE: %s took %sms", operation, duration);
            } else {
                LOG.debugf("PERFORMANCE: %s took %sms", operation, duration);
            }

            return result;
        } catch (final RuntimeException e) {
            final long endTime = System.currentTimeMillis();
            final long duration = endTime - startTime;
            LOG.errorf(e, "PERFORMANCE ERROR: %s failed after %sms", operation, duration);
            throw e;
        }
    }

    /**
     * Executes a runnable and logs the execution time
     * 
     * @param operation
     *            Description of the operation being timed
     * @param runnable
     *            The operation to execute
     */
    public static void time(final String operation, final Runnable runnable) {
        time(operation, () -> {
            runnable.run();
            return null;
        });
    }
}
