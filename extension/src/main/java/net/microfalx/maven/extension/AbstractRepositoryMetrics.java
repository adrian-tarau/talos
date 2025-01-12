package net.microfalx.maven.extension;

import net.microfalx.lang.TimeUtils;
import net.microfalx.maven.core.MavenUtils;
import net.microfalx.maven.model.ArtifactMetrics;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.Metadata;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;

/**
 * Base class for repository metrics.
 */
public abstract class AbstractRepositoryMetrics {

    protected static final Artifact NA = new DefaultArtifact("net.microfalx", "na", null, "0.0.0");

    private final Map<String, ArtifactMetrics> metrics = new ConcurrentHashMap<>();

    public ArtifactMetrics get(String id) {
        return metrics.get(id);
    }

    public Collection<ArtifactMetrics> getMetrics() {
        return unmodifiableCollection(metrics.values());
    }

    public Duration getResolutionDuration() {
        return TimeUtils.sum(getArtifactResolveDuration(), getArtifactInstallDuration(),
                getArtifactDeployDuration(),
                getMetadataResolvedDuration(), getMetadataDownloadDuration());
    }

    public int getMetadataResolvedCount() {
        return getMetrics().stream().mapToInt(ArtifactMetrics::getMetadataResolveCount).sum();
    }

    public int getArtifactDownloadCount() {
        return getMetrics().stream().mapToInt(ArtifactMetrics::getArtifactResolveCount).sum();
    }

    public Duration getMetadataResolvedDuration() {
        return TimeUtils.sum(getMetrics().stream().map(ArtifactMetrics::getMetadataResolveDuration));
    }

    public Duration getMetadataDownloadDuration() {
        return TimeUtils.sum(getMetrics().stream().map(ArtifactMetrics::getMetadataDownloadDuration));
    }

    public Duration getArtifactResolveDuration() {
        return TimeUtils.sum(getMetrics().stream().map(ArtifactMetrics::getArtifactResolveDuration));
    }

    public Duration getArtifactInstallDuration() {
        return TimeUtils.sum(getMetrics().stream().map(ArtifactMetrics::getArtifactResolveDuration));
    }

    public Duration getArtifactDeployDuration() {
        return TimeUtils.sum(getMetrics().stream().map(ArtifactMetrics::getArtifactDeployDuration));
    }

    public long getDownloadVolume() {
        return 0;
    }

    public long getUploadVolume() {
        return 0;
    }

    public Map<String, Collection<ArtifactMetrics>> getMetricsByGroup() {
        Map<String, Collection<ArtifactMetrics>> map = new TreeMap<>();
        for (ArtifactMetrics artifactMetric : getMetrics()) {
            map.computeIfAbsent(artifactMetric.getGroupId(), s -> new ArrayList<>()).add(artifactMetric);
        }
        return map;
    }

    protected final ArtifactMetrics getMetrics(Artifact artifact) {
        return metrics.computeIfAbsent(MavenUtils.getId(artifact), k -> new ArtifactMetrics(artifact));
    }

    protected ArtifactMetrics getMetrics(Metadata metadata) {
        return metrics.computeIfAbsent(MavenUtils.getId(metadata), k -> new ArtifactMetrics(metadata));
    }
}
