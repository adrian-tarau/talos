package net.microfalx.maven.core;

import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.metrics.Timer;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.time.Duration.ofNanos;
import static java.util.Collections.unmodifiableCollection;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.getStackTrace;
import static net.microfalx.maven.core.MavenUtils.METRICS;

/**
 * Tracks and times activities of a class. The extension should not fail, and we should
 * also report how much time is lost in the extension.
 */
public class MavenTracker {

    private final Class<?> clazz;
    private final org.slf4j.Logger logger;

    private final static Collection<Failure> failures = new LinkedBlockingDeque<>();

    /**
     * Returns all failures registered by instances of the tracker.
     *
     * @return a non-null instance
     */
    public static Collection<Failure> getFailures() {
        return unmodifiableCollection(failures);
    }

    /**
     * Resets the trackers.
     */
    public static void reset() {
        failures.clear();
    }

    public MavenTracker(Class<?> clazz) {
        requireNonNull(clazz);
        this.clazz = clazz;
        logger = LoggerFactory.getLogger(clazz);
    }

    public <T> void track(String name, Consumer<T> consumer) {
        track(name, consumer, null);
    }

    public <T> void track(String name, Consumer<T> consumer, MavenProject project) {
        track(name, consumer, project, null);
    }

    public <T> void track(String name, Consumer<T> consumer, MavenProject project, Mojo mojo) {
        try {
            METRICS.time(name, (t) -> consumer.accept(null));
        } catch (Exception e) {
            failures.add(new Failure(name, project, mojo, e));
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

    public void logFailure(String name, Throwable throwable) {
        String stackTrace = StringUtils.EMPTY_STRING;
        if (throwable != null) stackTrace = ", stack trace\n" + getStackTrace(throwable);
        logger.error("Failed action '{}' in '{}'{}", name, ClassUtils.getName(clazz), stackTrace);
    }

    public static class Failure {

        private final String name;
        private final MavenProject project;
        private final Mojo mojo;
        private final Throwable throwable;

        Failure(String name, MavenProject project, Mojo mojo, Throwable throwable) {
            this.name = name;
            this.project = project;
            this.mojo = mojo;
            this.throwable = throwable;
        }

        public String getName() {
            return name;
        }

        public MavenProject getProject() {
            return project;
        }

        public Mojo getMojo() {
            return mojo;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
