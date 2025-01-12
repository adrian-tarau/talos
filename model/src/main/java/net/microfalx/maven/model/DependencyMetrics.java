package net.microfalx.maven.model;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Holds metrics about a project dependency.
 */
public final class DependencyMetrics extends net.microfalx.maven.model.Dependency {

    private final Set<String> versions = new HashSet<>();
    private final Set<Project> projects = new HashSet<>();

    protected DependencyMetrics() {
    }

    public DependencyMetrics(Dependency dependency) {
        super(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        versions.add(dependency.getVersion());
    }

    public Set<String> getVersions() {
        return unmodifiableSet(versions);
    }

    public Set<Project> getProjects() {
        return unmodifiableSet(projects);
    }

    public void register(MavenProject project, Dependency dependency) {
        requireNonNull(project);
        requireNonNull(dependency);
        this.versions.add(dependency.getVersion());
        this.projects.add(new Project(project));
    }
}
