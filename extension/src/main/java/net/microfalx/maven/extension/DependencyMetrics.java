package net.microfalx.maven.extension;

import net.microfalx.lang.Identifiable;
import net.microfalx.lang.Nameable;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Holds metrics about a project dependency.
 */
public class DependencyMetrics implements Identifiable<String>, Nameable {

    private final String id;
    private final String groupId;
    private final String artifactId;

    private final Set<String> versions = new HashSet<>();
    private final Set<MavenProject> projects = new HashSet<>();

    DependencyMetrics(Dependency dependency) {
        requireNonNull(dependency);
        this.groupId = dependency.getGroupId();
        this.artifactId = dependency.getArtifactId();
        this.id = MavenUtils.getId(dependency);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return groupId + ":" + artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Set<String> getVersions() {
        return unmodifiableSet(versions);
    }

    public Set<MavenProject> getProjects() {
        return unmodifiableSet(projects);
    }

    void register(MavenProject project, Dependency dependency) {
        requireNonNull(project);
        requireNonNull(dependency);
        this.versions.add(dependency.getVersion());
        this.projects.add(project);
    }
}
