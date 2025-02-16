package net.microfalx.talos.model;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Holds metrics about a project dependency.
 */
public final class DependencyMetrics extends net.microfalx.talos.model.Dependency {

    private final Set<String> versions = new HashSet<>();
    private final Set<Project> projects = new HashSet<>();

    private long size = -1;
    private Duration duration;

    protected DependencyMetrics() {
    }

    public DependencyMetrics(Dependency dependency) {
        super(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        versions.add(dependency.getVersion());
    }

    public long getSize() {
        return size;
    }

    public DependencyMetrics setSize(long size) {
        this.size = size;
        return this;
    }

    public Duration getDuration() {
        return duration;
    }

    public DependencyMetrics setDuration(Duration duration) {
        this.duration = duration;
        return this;
    }

    public Set<String> getVersions() {
        return unmodifiableSet(versions);
    }

    public Set<Project> getProjects() {
        return unmodifiableSet(projects);
    }

    public DependencyMetrics register(MavenProject project, Dependency dependency) {
        requireNonNull(project);
        requireNonNull(dependency);
        this.versions.add(dependency.getVersion());
        this.projects.add(new Project(project, false));
        return this;
    }
}
