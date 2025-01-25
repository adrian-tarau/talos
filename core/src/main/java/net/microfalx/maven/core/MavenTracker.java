package net.microfalx.maven.core;

import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.metrics.Timer;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.time.Duration.ofNanos;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.maven.core.MavenUtils.METRICS;

/**
 * Tracks and times activities of a class. The extension should not fail, and we should
 * also report how much time is lost in the extension.
 */
public class MavenTracker {

    private final Class<?> clazz;
    private final org.slf4j.Logger logger;

    private static final AtomicInteger FAILURE_COUNT = new AtomicInteger();

    /**
     * Returns the number of encountered failures.
     *
     * @return a positive integer
     */
    public static int getFailureCount() {
        return FAILURE_COUNT.get();
    }

    public MavenTracker(Class<?> clazz) {
        requireNonNull(clazz);
        this.clazz = clazz;
        logger = LoggerFactory.getLogger(clazz);
    }

    public <T> void track(String name, Consumer<T> consumer) {
        try {
            METRICS.time(name, (t) -> consumer.accept(null));
        } catch (Exception e) {
            logFailure(name, e);
        }
    }

    public <T> void track(String name, Supplier<T> supplier) {
        try {
            METRICS.time(name, supplier);
        } catch (Exception e) {
            logFailure(name, e);
        }
    }

    public Duration getDuration() {
        return ofNanos(METRICS.getTimers().stream().map(Timer::getDuration).mapToLong(Duration::toNanos).sum());
    }

    private void logFailure(String name, Throwable throwable) {
        FAILURE_COUNT.incrementAndGet();
        String stackTrace = StringUtils.EMPTY_STRING;
        if (throwable != null) {
            stackTrace = ", stack trace\n" + ExceptionUtils.getStackTrace(throwable);
        }
        logger.error("Failed action '{}' in '{}'{}", name, ClassUtils.getName(clazz), stackTrace);
    }
}
