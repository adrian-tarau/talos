package net.microfalx.talos.extension;

import lombok.extern.slf4j.Slf4j;
import net.microfalx.jvm.model.Server;
import net.microfalx.jvm.model.VirtualMachine;
import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.talos.core.MavenLogger;
import net.microfalx.talos.model.ArtifactMetrics;
import net.microfalx.talos.model.SessionMetrics;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public abstract class AbstractExtensionTestCase {

    private MavenSession session;
    private MavenLogger logger;

    public MavenSession getSession() {
        return session;
    }

    public MavenLogger getLogger() {
        return logger;
    }

    protected final MavenSession initSession() {
        try {
            session = createSession("test");
            initMavenLogger();
            return session;
        } catch (MavenExecutionException e) {
            return ExceptionUtils.throwException(e);
        }
    }

    protected final SessionMetrics create() {
        SessionMetrics session = new SessionMetrics(createSession("Single"));
        session.setArtifacts(List.of(createArtifact()));
        session.setVirtualMachine(VirtualMachine.get());
        session.setServer(Server.get());
        return session;
    }

    protected final ArtifactMetrics createArtifact() {
        DefaultArtifact artifact = new DefaultArtifact("net.microfalx.maven", "test", "jar", "1.0.0");
        ArtifactMetrics metrics = new ArtifactMetrics(artifact);
        metrics.artifactResolveStart(artifact);
        metrics.artifactResolveStop(artifact, null);
        return metrics;
    }

    protected final MavenSession createSession(String name) {
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        DefaultMavenExecutionResult result = new DefaultMavenExecutionResult();
        MavenSession session = new MavenSession(null, request, result, createProject(name));
        return session;
    }


    protected final MavenProject createProject(String name) {
        MavenProject project = new MavenProject();
        project.setName(name);
        project.setGroupId("net.microfalx.maven");
        project.setArtifactId(StringUtils.toIdentifier(name));
        project.setVersion("1.0." + ThreadLocalRandom.current().nextInt(10));
        return project;
    }

    private void initMavenLogger() throws MavenExecutionException {
        logger = new MavenLogger();
        logger.afterSessionStart(session);
    }
}
