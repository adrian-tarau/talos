package net.microfalx.maven.model;

import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.maven.core.MavenUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.Duration.ofNanos;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Holds metrics about Mojo execution.
 */
public final class MojoMetrics extends NamedIdentityAware<String> {

    private String name;
    private String className;
    private volatile Duration duration;
    private final Set<String> goals = new HashSet<>();
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong durationNano = new AtomicLong(0);
    private FailureMetrics failureMetrics;

    private static final ThreadLocal<Long> startTime = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<Long> endTime = new ThreadLocal<>();

    private transient Mojo mojo;

    protected MojoMetrics() {
    }

    public MojoMetrics(Mojo mojo) {
        requireNonNull(mojo);
        this.className = ClassUtils.getName(mojo);
        setId(MavenUtils.getId(mojo));
        setName(MavenUtils.getName(mojo));
        this.mojo = mojo;
    }

    public String getGoal() {
        return String.join(", ", goals);
    }

    public String getClassName() {
        return className;
    }

    public void start(MojoExecution execution) {
        startTime.set(System.nanoTime());
        goals.add(MavenUtils.getGoal(execution));
    }

    public void stop(MavenProject project, Throwable throwable) {
        endTime.set(System.nanoTime());
        if (throwable != null) {
            this.failureMetrics = new FailureMetrics(project, mojo, null, throwable);
        }
        if (throwable != null) failureCount.incrementAndGet();
        durationNano.addAndGet(endTime.get() - startTime.get());
        executionCount.incrementAndGet();
    }

    public FailureMetrics getFailureMetrics() {
        return failureMetrics;
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public Duration getDuration() {
        if (this.duration == null) this.duration = ofNanos(durationNano.get());
        return this.duration;
    }

    public Duration getAverageDuration() {
        return getExecutionCount() > 0 ? getDuration().dividedBy(getExecutionCount()) : Duration.ZERO;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MojoMetrics.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("className='" + className + "'")
                .add("goals=" + goals)
                .add("duration=" + duration)
                .add("executionCount=" + executionCount)
                .add("failureCount=" + failureCount)
                .toString();
    }
}
