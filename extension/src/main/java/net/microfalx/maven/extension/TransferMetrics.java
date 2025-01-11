package net.microfalx.maven.extension;

import net.microfalx.lang.StringUtils;
import net.microfalx.maven.core.MavenConfiguration;
import net.microfalx.maven.core.MavenLogger;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * A class which tracks repository activity.
 */
@Named
@Singleton
public class TransferMetrics extends AbstractRepositoryMetrics implements TransferListener {

    private static final MavenLogger LOGGER = MavenLogger.create(TransferMetrics.class);

    @Inject
    protected MavenSession session;

    private MavenConfiguration configuration;
    private TransferListener listener;

    @PostConstruct
    public void postInit() {
        this.configuration = new MavenConfiguration(session);
    }

    TransferMetrics intercept(TransferListener listener) {
        requireNonNull(listener);
        this.listener = listener;
        return this;
    }

    @Override
    public void transferInitiated(TransferEvent event) throws TransferCancelledException {
        if (shouldForwardEvents()) listener.transferInitiated(event);
        trackEvent(event);
    }

    @Override
    public void transferStarted(TransferEvent event) throws TransferCancelledException {
        if (shouldForwardEvents()) listener.transferStarted(event);
    }

    @Override
    public void transferProgressed(TransferEvent event) throws TransferCancelledException {
        if (shouldForwardEvents()) listener.transferProgressed(event);
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        if (shouldForwardEvents()) listener.transferSucceeded(event);
        trackEvent(event);
    }

    @Override
    public void transferCorrupted(TransferEvent event) throws TransferCancelledException {
        if (shouldForwardEvents()) listener.transferCorrupted(event);
    }

    @Override
    public void transferFailed(TransferEvent event) {
        if (shouldForwardEvents()) listener.transferFailed(event);
        trackEvent(event);
    }

    private Artifact convertArtifact(TransferEvent event) {
        String[] parts = StringUtils.split(event.getResource().getResourceName(), "/");
        if (parts.length < 3) return null;
        return new DefaultArtifact(parts[0], parts[1], null, parts[2]);
    }

    private Metadata convertMetadata(TransferEvent event) {
        String[] parts = StringUtils.split(event.getResource().getResourceName(), "/");
        if (parts.length < 3) return null;
        return new DefaultMetadata(parts[0], parts[1], parts[2], null, Metadata.Nature.RELEASE_OR_SNAPSHOT);
    }

    private ArtifactMetrics getMetrics(TransferEvent event) {
        Artifact artifact = convertArtifact(event);
        return getMetrics(artifact == null ? NA : artifact);
    }

    private RepositoryEvent.EventType getEventType(TransferEvent event) {
        TransferEvent.EventType type = event.getType();
        String resourceName = event.getResource().getResourceName();
        boolean isMetadata = resourceName.endsWith("metadata.xml");
        boolean isArtifact = resourceName.endsWith(".jar");
        switch (type) {
            case INITIATED:
                return isArtifact ? RepositoryEvent.EventType.ARTIFACT_RESOLVING : RepositoryEvent.EventType.METADATA_RESOLVING;
            case FAILED:
            case SUCCEEDED:
            case CORRUPTED:
                return isArtifact ? RepositoryEvent.EventType.ARTIFACT_RESOLVED : RepositoryEvent.EventType.METADATA_RESOLVED;
            default:
                return null;
        }
    }

    private void trackEvent(TransferEvent event) {
        RepositoryEvent.EventType eventType = getEventType(event);
        if (eventType == null) return;
        Artifact artifact = null;
        Metadata metadata = null;
        ArtifactMetrics metrics = getMetrics(event);
        switch (eventType) {
            case ARTIFACT_RESOLVING:
                artifact = convertArtifact(event);
                if (artifact != null) metrics.artifactResolveStart(artifact);
                break;
            case ARTIFACT_RESOLVED:
                artifact = convertArtifact(event);
                if (artifact != null) metrics.artifactResolveStop(artifact, null);
                break;
            case METADATA_RESOLVING:
                metadata = convertMetadata(event);
                if (metadata != null) metrics.metadataResolveStart(metadata);
                break;
            case METADATA_RESOLVED:
                metadata = convertMetadata(event);
                if (metadata != null) metrics.metadataResolveStop(null);
                break;
        }
        LOGGER.debug("Track transfer: " + event.getResource().getResourceName() + ", event type: " + eventType
                     + ", artifact: " + artifact + ", metadata: " + metadata);
    }

    private boolean shouldForwardEvents() {
        return false;//!configuration.isQuiet();
    }
}
