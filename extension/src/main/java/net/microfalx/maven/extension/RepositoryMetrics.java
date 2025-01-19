package net.microfalx.maven.extension;

import net.microfalx.maven.core.MavenTracker;
import org.apache.maven.eventspy.EventSpy;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Collects metrics about various Maven events.
 */
@Named
@Singleton
public class RepositoryMetrics extends AbstractRepositoryMetrics implements EventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryMetrics.class);

    private final MavenTracker tracker = new MavenTracker(RepositoryMetrics.class);

    @Override
    public void init(Context context) throws Exception {
        // empty on purpose
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof RepositoryEvent) {
            tracker.track("Repository", t -> repositoryEvent((RepositoryEvent) event));
        }
    }

    @Override
    public void close() throws Exception {
        // empty on purpose
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
