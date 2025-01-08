package net.microfalx.maven.extension;

import net.microfalx.lang.Identifiable;
import net.microfalx.lang.Nameable;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.nanoTime;
import static java.time.Duration.ofNanos;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Holds metrics about Mojo execution.
 */
public class ArtifactMetrics implements Identifiable<String>, Nameable {

    private final String id;
    private final String groupId;
    private final String artifactId;

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

    ArtifactMetrics(Artifact artifact) {
        requireNonNull(artifact);
        this.groupId = artifact.getGroupId();
        this.artifactId = artifact.getArtifactId();
        this.id = MavenUtils.getId(artifact);
    }

    ArtifactMetrics(Metadata metadata) {
        requireNonNull(metadata);
        this.groupId = metadata.getGroupId();
        this.artifactId = metadata.getArtifactId();
        this.id = MavenUtils.getId(metadata);
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

    void artifactResolveStart(Artifact artifact) {
        reset();
        artifactResolveCount.incrementAndGet();
        artifactResolveStartTime.set(nanoTime());
        versions.add(artifact.getVersion());
    }

    void artifactResolveStop(Artifact artifact, Throwable throwable) {
        artifactResolveEndTime.set(nanoTime());
        artifactResolveDurationSum.addAndGet(artifactResolveEndTime.get() - artifactResolveStartTime.get());
        if (artifact.getFile() != null) size = artifact.getFile().length();
    }

    void artifactInstallStart(Artifact artifact) {
        reset();
        artifactInstallCount.incrementAndGet();
        artifactInstallStartTime.set(nanoTime());
    }

    void artifactInstallStop(Throwable throwable) {
        artifactInstallEndTime.set(nanoTime());
        artifactInstallDurationSum.addAndGet(artifactInstallEndTime.get() - artifactInstallStartTime.get());
    }

    void artifactDeployStart(Artifact artifact) {
        reset();
        artifactDeployCount.incrementAndGet();
        artifactDeployStartTime.set(nanoTime());
        versions.add(artifact.getVersion());
    }

    void artifactDeployStop(Throwable throwable) {
        artifactDeployEndTime.set(nanoTime());
        artifactDeployDurationSum.addAndGet(artifactDeployEndTime.get() - artifactDeployStartTime.get());
    }

    void metadataResolveStart(Metadata metadata) {
        reset();
        metadataResolveCount.incrementAndGet();
        metadataResolveStartTime.set(nanoTime());
        versions.add(metadata.getVersion());
    }

    void metadataResolveStop(Throwable throwable) {
        metadataResolveEndTime.set(nanoTime());
        metadataResolveDurationSum.addAndGet(metadataResolveEndTime.get() - metadataResolveStartTime.get());
    }

    void metadataDownloadStart(Metadata metadata) {
        reset();
        metadataDownloadCount.incrementAndGet();
        metadataDownloadStartTime.set(nanoTime());
        versions.add(metadata.getVersion());
    }

    void metadataDownloadStop(Throwable throwable) {
        metadataDownloadEndTime.set(nanoTime());
        metadataDownloadDurationSum.addAndGet(metadataDownloadStartTime.get() - metadataDownloadStartTime.get());
    }

    Duration getArtifactResolveDuration() {
        if (this.artifactResolve == null) this.artifactResolve = ofNanos(artifactResolveDurationSum.get());
        return this.artifactResolve;
    }

    Duration getArtifactInstallDuration() {
        if (this.artifactInstall == null) this.artifactInstall = ofNanos(artifactInstallDurationSum.get());
        return this.artifactInstall;
    }

    Duration getArtifactDeployDuration() {
        if (this.artifactDeploy == null) this.artifactDeploy = ofNanos(artifactDeployDurationSum.get());
        return this.artifactDeploy;
    }

    Duration getMetadataResolveDuration() {
        if (this.metadataResolve == null) this.metadataResolve = ofNanos(metadataResolveDurationSum.get());
        return this.metadataResolve;
    }

    Duration getMetadataDownloadDuration() {
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
