package net.microfalx.talos.junit;

import net.microfalx.lang.StringUtils;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

class SurefireTestsTest {

    @Test
    void load() {
        MavenSession session = createSession("test");
        SurefireTests tests = new SurefireTests();
        tests.load(session);
    }

    protected final MavenSession createSession(String name) {
        DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
        DefaultMavenExecutionResult result = new DefaultMavenExecutionResult();
        MavenSession session = new MavenSession(null, request, result, createProject(name));
        session.setProjects(List.of(createProject(name)));
        return session;
    }


    protected final MavenProject createProject(String name) {
        MavenProject project = new MavenProject();
        project.setName(name);
        project.setGroupId("net.microfalx.talos");
        project.setArtifactId(StringUtils.toIdentifier(name));
        project.setVersion("1.0." + ThreadLocalRandom.current().nextInt(10));
        Build build = new Build();
        build.setDirectory(new File("src/test/target").getAbsolutePath());
        project.setBuild(build);
        return project;
    }
}