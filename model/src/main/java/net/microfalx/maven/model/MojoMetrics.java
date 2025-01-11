package net.microfalx.maven.model;

import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.Nameable;
import net.microfalx.maven.core.MavenUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.Duration.ofNanos;

/**
 * Holds metrics about Mojo execution.
 */
public final class MojoMetrics implements Nameable {

    private final String name;
    private final Class<?> clazz;
    private volatile Duration duration;
    private final Set<String> goals = new HashSet<>();
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong durationNano = new AtomicLong(0);
    private volatile Throwable throwable;

    private static final ThreadLocal<Long> startTime = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<Long> endTime = new ThreadLocal<>();

    public MojoMetrics(Mojo mojo) {
        this.clazz = mojo.getClass();
        this.name = MavenUtils.getName(mojo);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getGoal() {
        return String.join(", ", goals);
    }

    public String getClassName() {
        return ClassUtils.getName(clazz);
    }

    public void start(MojoExecution execution) {
        startTime.set(System.nanoTime());
        goals.add(MavenUtils.getGoal(execution));
    }

    public void stop(Throwable throwable) {
        endTime.set(System.nanoTime());
        this.throwable = throwable;
        if (throwable != null) failureCount.incrementAndGet();
        durationNano.addAndGet(endTime.get() - startTime.get());
        executionCount.incrementAndGet();
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
}
