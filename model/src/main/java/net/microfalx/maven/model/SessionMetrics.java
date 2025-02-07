package net.microfalx.maven.model;

import net.microfalx.metrics.SeriesStore;
import net.microfalx.resource.Resource;
import org.apache.maven.execution.MavenSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableCollection;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.NA_STRING;

/**
 * Holds metrics about a Maven session.
 */
public class SessionMetrics extends AbstractSessionMetrics {

    private final Collection<ArtifactMetrics> artifacts = new ArrayList<>();
    private final Collection<DependencyMetrics> dependencies = new ArrayList<>();
    private final Collection<PluginMetrics> plugins = new ArrayList<>();
    private final Collection<TestMetrics> tests = new ArrayList<>();
    private final Collection<TrendMetrics> trends = new ArrayList<>();

    private SeriesStore virtualMachineMetrics = SeriesStore.memory();
    private SeriesStore serverMetrics = SeriesStore.memory();

    private String logs;
    private boolean testsUpdated;

    public static SessionMetrics load(Resource resource) throws IOException {
        return AbstractSessionMetrics.load(resource, SessionMetrics.class);
    }

    public static SessionMetrics load(InputStream inputStream) throws IOException {
        return AbstractSessionMetrics.load(inputStream, SessionMetrics.class);
    }

    protected SessionMetrics() {
    }

    public SessionMetrics(MavenSession session) {
        super(session);
    }

    public Collection<ArtifactMetrics> getArtifacts() {
        return unmodifiableCollection(artifacts);
    }

    private Collection<ArtifactMetrics> getTrimmedArtifacts() {
        if (isVerbose()) {
            return getArtifacts();
        } else {
            return getArtifacts().stream()
                    .filter(a -> a.getDuration().toMillis() > 5)
                    .collect(Collectors.toList());
        }
    }

    public void setArtifacts(Collection<ArtifactMetrics> artifacts) {
        requireNonNull(artifacts);
        this.artifacts.addAll(artifacts);
    }

    public Collection<DependencyMetrics> getDependencies() {
        return unmodifiableCollection(dependencies);
    }

    public void setDependencies(Collection<DependencyMetrics> dependencies) {
        requireNonNull(dependencies);
        this.dependencies.addAll(dependencies);
    }

    public Collection<PluginMetrics> getPlugins() {
        return unmodifiableCollection(plugins);
    }

    public void setPlugins(Collection<PluginMetrics> plugins) {
        requireNonNull(plugins);
        this.plugins.addAll(plugins);
    }

    public Collection<TrendMetrics> getTrends() {
        return unmodifiableCollection(trends);
    }

    public void setTrends(Collection<TrendMetrics> trends) {
        requireNonNull(trends);
        this.trends.addAll(trends);
    }

    public Collection<TestMetrics> getTests() {
        if (!testsUpdated) {
            tests.forEach(this::updateTestMetrics);
            testsUpdated = true;
        }
        return unmodifiableCollection(tests);
    }

    public void setTests(Collection<TestMetrics> tests) {
        requireNonNull(tests);
        this.tests.addAll(tests);
    }

    public SeriesStore getVirtualMachineMetrics() {
        return virtualMachineMetrics;
    }

    public void setVirtualMachineMetrics(SeriesStore virtualMachineMetrics) {
        this.virtualMachineMetrics = virtualMachineMetrics;
    }

    public SeriesStore getServerMetrics() {
        return serverMetrics;
    }

    public void setServerMetrics(SeriesStore serverMetrics) {
        this.serverMetrics = serverMetrics;
    }


    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        requireNonNull(logs);
        this.logs = logs;
    }

    private void updateTestMetrics(TestMetrics test) {
        if (test.getModuleId() != null && test.getModule() == null) {
            test.module = getModule(test.getModuleId());
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SessionMetrics.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("artifacts=" + artifacts.size())
                .add("dependencies=" + dependencies.size())
                .add("plugins=" + plugins.size())
                .add("log='" + (logs != null ? logs.length() : NA_STRING) + "'")
                .toString();
    }


}
