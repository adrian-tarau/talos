package net.microfalx.maven.extension;

import net.microfalx.lang.TimeUtils;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;

/**
 * Collects metrics about various Maven events.
 */
@Named
@Singleton
public class RepositoryMetrics extends AbstractEventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryMetrics.class);

    private final Map<String, ArtifactMetrics> artifactMetrics = new ConcurrentHashMap<>();

    protected ArtifactMetrics get(String id) {
        return artifactMetrics.get(id);
    }

    protected Collection<ArtifactMetrics> getArtifactMetrics() {
        return unmodifiableCollection(artifactMetrics.values());
    }

    protected Duration getArtifactResolutionDuration() {
        return TimeUtils.sum(getArtifactResolveDuration(), getArtifactInstallDuration(),
                getArtifactDeployDuration(),
                getMetadataResolvedDuration(), getMetadataDownloadDuration());
    }

    protected int getMetadataResolvedCount() {
        return getArtifactMetrics().stream().mapToInt(ArtifactMetrics::getMetadataResolveCount).sum();
    }

    protected int getArtifactDownloadCount() {
        return getArtifactMetrics().stream().mapToInt(ArtifactMetrics::getArtifactResolveCount).sum();
    }

    protected Duration getMetadataResolvedDuration() {
        return TimeUtils.sum(getArtifactMetrics().stream().map(ArtifactMetrics::getMetadataResolveDuration));
    }

    protected Duration getMetadataDownloadDuration() {
        return TimeUtils.sum(getArtifactMetrics().stream().map(ArtifactMetrics::getMetadataDownloadDuration));
    }

    protected Duration getArtifactResolveDuration() {
        return TimeUtils.sum(getArtifactMetrics().stream().map(ArtifactMetrics::getArtifactResolveDuration));
    }

    protected Duration getArtifactInstallDuration() {
        return TimeUtils.sum(getArtifactMetrics().stream().map(ArtifactMetrics::getArtifactResolveDuration));
    }

    protected Duration getArtifactDeployDuration() {
        return TimeUtils.sum(getArtifactMetrics().stream().map(ArtifactMetrics::getArtifactDeployDuration));
    }

    protected Map<String, Collection<ArtifactMetrics>> getArtifactMetricsByGroup() {
        Map<String, Collection<ArtifactMetrics>> map = new TreeMap<>();
        for (ArtifactMetrics artifactMetric : getArtifactMetrics()) {
            map.computeIfAbsent(artifactMetric.getGroupId(), s -> new ArrayList<>()).add(artifactMetric);
        }
        return map;
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof RepositoryEvent) {
            repositoryEvent((RepositoryEvent) event);
        }
    }

    private ArtifactMetrics getMetrics(Artifact artifact) {
        return artifactMetrics.computeIfAbsent(MavenUtils.getId(artifact), k -> new ArtifactMetrics(artifact));
    }

    private ArtifactMetrics getMetrics(Metadata metadata) {
        return artifactMetrics.computeIfAbsent(MavenUtils.getId(metadata), k -> new ArtifactMetrics(metadata));
    }

    private void repositoryEvent(RepositoryEvent repositoryEvent) {
        Artifact artifact = repositoryEvent.getArtifact();
        Metadata metadata = repositoryEvent.getMetadata();
        switch (repositoryEvent.getType()) {
            case ARTIFACT_RESOLVING:
                getMetrics(artifact).artifactResolveStart(artifact);
                break;
            case ARTIFACT_RESOLVED:
                getMetrics(artifact).artifactResolveStop(artifact, repositoryEvent.getException());
                break;
            case ARTIFACT_INSTALLING:
                getMetrics(artifact).artifactInstallStart(artifact);
                break;
            case ARTIFACT_INSTALLED:
                getMetrics(artifact).artifactInstallStop(repositoryEvent.getException());
                break;
            case ARTIFACT_DEPLOYING:
                getMetrics(artifact).artifactDeployStart(artifact);
                break;
            case ARTIFACT_DEPLOYED:
                getMetrics(artifact).artifactDeployStop(repositoryEvent.getException());
                break;
            case METADATA_RESOLVING:
                getMetrics(metadata).metadataResolveStart(metadata);
                break;
            case METADATA_RESOLVED:
                getMetrics(metadata).metadataResolveStop(repositoryEvent.getException());
                break;
            case METADATA_DOWNLOADING:
                getMetrics(metadata).metadataDownloadStart(metadata);
                break;
            case METADATA_DOWNLOADED:
                getMetrics(metadata).metadataDownloadStop(repositoryEvent.getException());
                break;
        }
    }
}
