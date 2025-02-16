package net.microfalx.talos.model;

import net.microfalx.metrics.SeriesStore;
import net.microfalx.resource.Resource;
import org.apache.maven.execution.MavenSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.unmodifiableCollection;

public class TrendMetrics extends AbstractSessionMetrics<TrendMetrics> {

    private Collection<TestSummaryMetrics> tests;
    private Collection<ArtifactSummaryMetrics> artifacts;

    private SeriesStore virtualMachineMetrics = SeriesStore.memory();
    private SeriesStore serverMetrics = SeriesStore.memory();

    public static TrendMetrics load(Resource resource) throws IOException {
        return AbstractSessionMetrics.load(resource, TrendMetrics.class);
    }

    public static TrendMetrics load(InputStream inputStream) throws IOException {
        return AbstractSessionMetrics.load(inputStream, TrendMetrics.class);
    }

    protected TrendMetrics() {
    }

    public TrendMetrics(MavenSession session) {
        super(session);
    }

    public Collection<TestSummaryMetrics> getTests() {
        return unmodifiableCollection(tests);
    }

    public Collection<ArtifactSummaryMetrics> getArtifacts() {
        return unmodifiableCollection(artifacts);
    }

    public SeriesStore getVirtualMachineMetrics() {
        return virtualMachineMetrics;
    }

    public SeriesStore getServerMetrics() {
        return serverMetrics;
    }

    public static TrendMetrics from(SessionMetrics sessionMetrics) {
        TrendMetrics trendMetrics = new TrendMetrics();
        copy(sessionMetrics, trendMetrics);
        trendMetrics.virtualMachineMetrics = getAverageStore(sessionMetrics.getVirtualMachineMetrics());
        trendMetrics.serverMetrics = getAverageStore(sessionMetrics.getServerMetrics());
        trendMetrics.tests = TestSummaryMetrics.from(sessionMetrics.getTests());
        trendMetrics.artifacts = ArtifactSummaryMetrics.from(sessionMetrics.getArtifacts());
        return trendMetrics;
    }

    private static SeriesStore getAverageStore(SeriesStore source) {
        SeriesStore target = SeriesStore.memory();
        target.add(Collections.singleton(source), true);
        return target;
    }

}
