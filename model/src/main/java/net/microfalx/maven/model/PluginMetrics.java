package net.microfalx.maven.model;

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
public final class PluginMetrics extends Dependency {

    private final Set<String> versions = new HashSet<>();
    private final Set<String> goals = new HashSet<>();
    private final Set<Project> projects = new HashSet<>();

    protected PluginMetrics() {
    }

    public PluginMetrics(Plugin plugin) {
        super(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
        this.versions.add(plugin.getVersion());
    }

    public Set<String> getVersions() {
        return unmodifiableSet(versions);
    }

    public Set<String> getGoals() {
        return unmodifiableSet(goals);
    }

    public Set<Project> getProjects() {
        return unmodifiableSet(projects);
    }

    public void registerGoal(String goal) {
        if (StringUtils.isNotEmpty(goal)) this.goals.add(goal);
    }

    public void register(MavenProject project, Plugin plugin) {
        requireNonNull(project);
        requireNonNull(plugin);
        this.versions.add(plugin.getVersion());
        this.projects.add(new Project(project, false));
    }
}
