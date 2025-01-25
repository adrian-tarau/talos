package net.microfalx.maven.report;

import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.FormatterUtils;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.lang.TimeUtils;
import net.microfalx.maven.core.MavenUtils;
import net.microfalx.maven.model.*;
import net.microfalx.resource.Resource;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

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

    public String formatDuration(Duration duration) {
        return MavenUtils.formatDuration(duration, false, false);
    }

    public long getFailureCount() {
        return session.getMojos().stream().mapToLong(MojoMetrics::getFailureCount).sum();
    }

    public long getExecutionCount() {
        return session.getMojos().stream().mapToLong(MojoMetrics::getExecutionCount).sum();
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

    public String getLogAsHtml() {
        AnsiToHtml ansiToHtml = new AnsiToHtml();
        try {
            Resource resource = ansiToHtml.transform(Resource.text(session.getLog()));
            return resource.loadAsString();
        } catch (IOException e) {
            return "#ERROR: " + ExceptionUtils.getRootCauseMessage(e);
        }
    }

}
