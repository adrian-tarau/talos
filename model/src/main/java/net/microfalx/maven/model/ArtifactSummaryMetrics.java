package net.microfalx.maven.model;

import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.lang.UriUtils;

import java.time.Duration;
import java.util.*;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;
import static net.microfalx.lang.StringUtils.toIdentifier;

public class ArtifactSummaryMetrics extends NamedIdentityAware<String> {

    private Duration metadataResolveDuration = Duration.ZERO;
    private Duration metadataDownloadDuration = Duration.ZERO;
    private Duration artifactResolveDuration = Duration.ZERO;
    private Duration artifactInstallDuration = Duration.ZERO;
    private Duration artifactDeployDuration = Duration.ZERO;

    private long size;

    protected ArtifactSummaryMetrics() {
    }

    protected ArtifactSummaryMetrics(String name) {
        requireNotEmpty(name);
        setId(toIdentifier(name));
        setName(name);
    }

    public Duration getMetadataResolveDuration() {
        return metadataResolveDuration;
    }

    public Duration getMetadataDownloadDuration() {
        return metadataDownloadDuration;
    }

    public Duration getArtifactResolveDuration() {
        return artifactResolveDuration;
    }

    public Duration getArtifactInstallDuration() {
        return artifactInstallDuration;
    }

    public Duration getArtifactDeployDuration() {
        return artifactDeployDuration;
    }

    public long getSize() {
        return size;
    }

    public static Collection<ArtifactSummaryMetrics> from(Collection<ArtifactMetrics> metrics) {
        Map<String, ArtifactSummaryMetrics> summaryMetrics = new HashMap<>();
        for (ArtifactMetrics metric : metrics) {
            ArtifactSummaryMetrics summary = summaryMetrics.computeIfAbsent(UriUtils.getTld(metric.getGroupId()), ArtifactSummaryMetrics::new);
            summary.add(metric);
        }
        return new ArrayList<>(summaryMetrics.values());
    }

    public void add(ArtifactMetrics metrics) {
        requireNonNull(metrics);
        metadataResolveDuration = metadataResolveDuration.plus(metrics.getMetadataResolveDuration());
        metadataDownloadDuration = metadataDownloadDuration.plus(metrics.getMetadataDownloadDuration());
        artifactResolveDuration = artifactResolveDuration.plus(metrics.getArtifactResolveDuration());
        artifactInstallDuration = artifactInstallDuration.plus(metrics.getArtifactInstallDuration());
        artifactDeployDuration = artifactDeployDuration.plus(metrics.getArtifactDeployDuration());
        size += metrics.getSize();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ArtifactSummaryMetrics.class.getSimpleName() + "[", "]")
                .add("name=" + getName())
                .add("metadataResolveDuration=" + metadataResolveDuration)
                .add("metadataDownloadDuration=" + metadataDownloadDuration)
                .add("artifactResolveDuration=" + artifactResolveDuration)
                .add("artifactInstallDuration=" + artifactInstallDuration)
                .add("artifactDeployDuration=" + artifactDeployDuration)
                .add("size=" + size)
                .toString();
    }
}
