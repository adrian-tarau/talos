package net.microfalx.talos.model;

import net.microfalx.lang.TimeUtils;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.nanoTime;
import static java.time.Duration.ofNanos;

/**
 * Holds metrics about Mojo execution.
 */
public final class ArtifactMetrics extends Dependency {

    private final Set<String> versions = new HashSet<>();

    private volatile long size;

    private volatile Duration metadataResolve;
    private volatile Duration metadataDownload;
    private volatile Duration artifactResolve;
    private volatile Duration artifactInstall;
    private volatile Duration artifactDeploy;
    private final AtomicInteger metadataResolveCount = new AtomicInteger(0);
    private final AtomicInteger metadataDownloadCount = new AtomicInteger(0);
    private final AtomicInteger artifactResolveCount = new AtomicInteger(0);
    private final AtomicInteger artifactInstallCount = new AtomicInteger(0);
    private final AtomicInteger artifactDeployCount = new AtomicInteger(0);
    private final AtomicLong metadataResolveDurationSum = new AtomicLong(0);
    private final AtomicLong metadataDownloadDurationSum = new AtomicLong(0);
    private final AtomicLong artifactResolveDurationSum = new AtomicLong(0);
    private final AtomicLong artifactInstallDurationSum = new AtomicLong(0);
    private final AtomicLong artifactDeployDurationSum = new AtomicLong(0);

    private static final ThreadLocal<Long> metadataResolveStartTime = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<Long> metadataResolveEndTime = new ThreadLocal<>();
    private static final ThreadLocal<Long> metadataDownloadStartTime = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<Long> metadataDownloadEndTime = new ThreadLocal<>();
    private static final ThreadLocal<Long> artifactResolveStartTime = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<Long> artifactResolveEndTime = new ThreadLocal<>();
    private static final ThreadLocal<Long> artifactInstallStartTime = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<Long> artifactInstallEndTime = new ThreadLocal<>();
    private static final ThreadLocal<Long> artifactDeployStartTime = ThreadLocal.withInitial(System::nanoTime);
    private static final ThreadLocal<Long> artifactDeployEndTime = new ThreadLocal<>();

    protected ArtifactMetrics() {
    }

    public ArtifactMetrics(Artifact artifact) {
        super(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        versions.add(artifact.getVersion());
    }

    public ArtifactMetrics(Metadata metadata) {
        super(metadata.getGroupId(), metadata.getArtifactId(), metadata.getVersion());
    }

    public long getSize() {
        return size;
    }

    public int getMetadataResolveCount() {
        return metadataResolveCount.get();
    }

    public int getMetadataDownloadCount() {
        return metadataDownloadCount.get();
    }

    public int getArtifactResolveCount() {
        return artifactResolveCount.get();
    }

    public int getArtifactInstallCount() {
        return artifactInstallCount.get();
    }

    public int getArtifactDeployCount() {
        return artifactDeployCount.get();
    }

    public void artifactResolveStart(Artifact artifact) {
        reset();
        artifactResolveCount.incrementAndGet();
        artifactResolveStartTime.set(nanoTime());
        versions.add(artifact.getVersion());
    }

    public void artifactResolveStop(Artifact artifact, Throwable throwable) {
        artifactResolveEndTime.set(nanoTime());
        artifactResolveDurationSum.addAndGet(artifactResolveEndTime.get() - artifactResolveStartTime.get());
        if (artifact.getFile() != null) size = artifact.getFile().length();
    }

    public void artifactInstallStart(Artifact artifact) {
        reset();
        artifactInstallCount.incrementAndGet();
        artifactInstallStartTime.set(nanoTime());
    }

    public void artifactInstallStop(Throwable throwable) {
        artifactInstallEndTime.set(nanoTime());
        artifactInstallDurationSum.addAndGet(artifactInstallEndTime.get() - artifactInstallStartTime.get());
    }

    public void artifactDeployStart(Artifact artifact) {
        reset();
        artifactDeployCount.incrementAndGet();
        artifactDeployStartTime.set(nanoTime());
        versions.add(artifact.getVersion());
    }

    public void artifactDeployStop(Throwable throwable) {
        artifactDeployEndTime.set(nanoTime());
        artifactDeployDurationSum.addAndGet(artifactDeployEndTime.get() - artifactDeployStartTime.get());
    }

    public void metadataResolveStart(Metadata metadata) {
        reset();
        metadataResolveCount.incrementAndGet();
        metadataResolveStartTime.set(nanoTime());
        versions.add(metadata.getVersion());
    }

    public void metadataResolveStop(Throwable throwable) {
        metadataResolveEndTime.set(nanoTime());
        metadataResolveDurationSum.addAndGet(metadataResolveEndTime.get() - metadataResolveStartTime.get());
    }

    public void metadataDownloadStart(Metadata metadata) {
        reset();
        metadataDownloadCount.incrementAndGet();
        metadataDownloadStartTime.set(nanoTime());
        versions.add(metadata.getVersion());
    }

    public void metadataDownloadStop(Throwable throwable) {
        metadataDownloadEndTime.set(nanoTime());
        metadataDownloadDurationSum.addAndGet(metadataDownloadStartTime.get() - metadataDownloadStartTime.get());
    }

    public Duration getDuration() {
        return TimeUtils.sum(getArtifactResolveDuration(), getArtifactInstallDuration(), getArtifactDeployDuration(),
                getMetadataResolveDuration(), getMetadataDownloadDuration());
    }

    public Duration getArtifactResolveDuration() {
        if (this.artifactResolve == null) this.artifactResolve = ofNanos(artifactResolveDurationSum.get());
        return this.artifactResolve;
    }

    public Duration getArtifactInstallDuration() {
        if (this.artifactInstall == null) this.artifactInstall = ofNanos(artifactInstallDurationSum.get());
        return this.artifactInstall;
    }

    public Duration getArtifactDeployDuration() {
        if (this.artifactDeploy == null) this.artifactDeploy = ofNanos(artifactDeployDurationSum.get());
        return this.artifactDeploy;
    }

    public Duration getMetadataResolveDuration() {
        if (this.metadataResolve == null) this.metadataResolve = ofNanos(metadataResolveDurationSum.get());
        return this.metadataResolve;
    }

    public Duration getMetadataDownloadDuration() {
        if (this.metadataDownload == null) this.metadataDownload = ofNanos(metadataDownloadDurationSum.get());
        return this.metadataDownload;
    }

    private void reset() {
        this.artifactResolve = null;
        this.artifactInstall = null;
        this.artifactDeploy = null;
        this.metadataResolve = null;
        this.metadataDownload = null;
    }
}
