package net.microfalx.maven.model;

import net.microfalx.lang.ClassUtils;
import net.microfalx.maven.core.MavenUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Holds metrics about Mojo execution.
 */
public final class MojoMetrics extends AbstractTimeAwareMetrics<MojoMetrics> {

    private String className;
    private final Set<String> goals = new CopyOnWriteArraySet<>();
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile FailureMetrics failureMetrics;

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
        setStartTime(ZonedDateTime.now());
        goals.add(MavenUtils.getGoal(execution));
    }

    public void stop(MavenProject project, Throwable throwable) {
        setEndTime(ZonedDateTime.now());
        if (throwable != null) {
            this.failureMetrics = new FailureMetrics(project, mojo, null, throwable);
        }
        if (throwable != null) failureCount.incrementAndGet();
    }

    public FailureMetrics getFailureMetrics() {
        return failureMetrics;
    }

    public int getFailureCount() {
        return failureCount.get();
    }
}
