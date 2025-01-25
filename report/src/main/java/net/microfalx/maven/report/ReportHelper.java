package net.microfalx.maven.report;

import net.microfalx.lang.*;
import net.microfalx.maven.core.MavenUtils;
import net.microfalx.maven.model.*;
import net.microfalx.resource.Resource;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class ReportHelper {

    private final SessionMetrics session;

    public ReportHelper(SessionMetrics session) {
        requireNonNull(session);
        this.session = session;
    }

    public String formatDateTime(Object temporal) {
        return FormatterUtils.formatDateTime(temporal);
    }

    public String formatBytes(Number value) {
        return FormatterUtils.formatBytes(value);
    }

    public String formatDuration(Duration duration) {
        return MavenUtils.formatDuration(duration, false, false);
    }

    public String toString(Object value) {
        if (value instanceof Collection<?>) {
            StringBuilder builder = new StringBuilder();
            for (Object o : (Collection<?>) value) {
                StringUtils.append(builder, o, ", ");
            }
            return builder.toString();
        } else {
            return ObjectUtils.toString(value);
        }
    }

    public long getFailureCount() {
        return session.getMojos().stream().mapToLong(MojoMetrics::getFailureCount).sum();
    }

    public String getBuildTime() {
        return TimeUtils.toString(session.getMojos().stream().map(MojoMetrics::getDuration).reduce(Duration.ZERO, Duration::plus));
    }

    public long getProjectCount() {
        return session.getModules().size();
    }

    public Collection<MojoMetrics> getMojos() {
        List<MojoMetrics> mojos = new ArrayList<>(session.getMojos());
        mojos.sort(Comparator.comparing(MojoMetrics::getDuration).reversed());
        return mojos;
    }

    public Collection<PluginMetrics> getPlugins() {
        List<PluginMetrics> plugins = new ArrayList<>(session.getPlugins());
        plugins.sort(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId));
        return plugins;
    }

    public Collection<DependencyMetrics> getDependencies() {
        List<DependencyMetrics> dependencies = new ArrayList<>(session.getDependencies());
        dependencies.sort(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId));
        return dependencies;
    }

    public Collection<ArtifactMetrics> getArtifacts() {
        List<ArtifactMetrics> artifacts = new ArrayList<>(session.getArtifacts());
        artifacts.sort(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId));
        return artifacts;
    }

    public Collection<ProjectMetrics> getModules() {
        List<ProjectMetrics> artifacts = new ArrayList<>(session.getModules());
        artifacts.sort(Comparator.comparing(NamedIdentityAware::getName));
        return artifacts;
    }

    public Collection<ProjectDetails> getProjectDetails() {
        Map<Project, ProjectDetails> projectDetails = new HashMap<>();
        for (PluginMetrics pluginMetrics : session.getPlugins()) {
            for (Project project : pluginMetrics.getProjects()) {
                ProjectDetails details = projectDetails.computeIfAbsent(project, ProjectDetails::new);
                details.plugins.add(pluginMetrics);
            }
        }
        List<ProjectDetails> details = new ArrayList<>(projectDetails.values());
        details.sort(Comparator.comparing(p -> p.getProject().getName()));
        return details;
    }

    public String getLogAsHtml() {
        AnsiToHtml ansiToHtml = new AnsiToHtml();
        try {
            Resource resource = ansiToHtml.transform(Resource.text(session.getLog()));
            return resource.loadAsString();
        } catch (IOException e) {
            return "#ERROR: " + ExceptionUtils.getRootCauseMessage(e);
        }
    }

    public static class ProjectDetails {

        private final Project project;
        private final Collection<PluginMetrics> plugins = new ArrayList<>();

        public ProjectDetails(Project project) {
            requireNonNull(project);
            this.project = project;
        }

        public Project getProject() {
            return project;
        }

        public Collection<PluginMetrics> getPlugins() {
            return plugins;
        }
    }

}
