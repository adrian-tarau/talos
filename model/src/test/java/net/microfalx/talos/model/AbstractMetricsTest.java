package net.microfalx.talos.model;

import net.microfalx.jvm.model.Server;
import net.microfalx.jvm.model.VirtualMachine;
import net.microfalx.lang.StringUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.DefaultArtifact;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractMetricsTest {

    protected final SessionMetrics create() {
        SessionMetrics session = new SessionMetrics(createSession("Single"));
        session.setArtifacts(List.of(createArtifact()));
        session.setVirtualMachine(VirtualMachine.get());
        session.setServer(Server.get());
        return session;
    }

    protected final ArtifactMetrics createArtifact() {
        DefaultArtifact artifact = new DefaultArtifact("net.microfalx.talos", "test", "jar", "1.0.0");
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
        project.setGroupId("net.microfalx.talos");
        project.setArtifactId(StringUtils.toIdentifier(name));
        project.setVersion("1.0." + ThreadLocalRandom.current().nextInt(10));
        return project;
    }
}
