package net.microfalx.maven.extension;

import net.microfalx.lang.Identifiable;
import net.microfalx.lang.Nameable;
import net.microfalx.lang.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Holds metrics about a project dependency.
 */
public class PluginMetrics implements Identifiable<String>, Nameable {

    private final String id;
    private final String groupId;
    private final String artifactId;

    private final Set<String> versions = new HashSet<>();
    private final Set<String> goals = new HashSet<>();
    private final Set<MavenProject> projects = new HashSet<>();

    PluginMetrics(Plugin plugin) {
        requireNonNull(plugin);
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.id = MavenUtils.getId(plugin);
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

    public Set<String> getGoals() {
        return unmodifiableSet(goals);
    }

    public Set<MavenProject> getProjects() {
        return unmodifiableSet(projects);
    }

    void registerGoal(String goal) {
        if (StringUtils.isNotEmpty(goal)) this.goals.add(goal);
    }

    void register(MavenProject project, Plugin plugin) {
        requireNonNull(project);
        requireNonNull(plugin);
        this.versions.add(plugin.getVersion());
        this.projects.add(project);
    }
}
