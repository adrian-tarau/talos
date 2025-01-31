package net.microfalx.maven.report;

import net.microfalx.maven.model.FailureMetrics;
import net.microfalx.maven.model.SessionMetrics;
import net.microfalx.resource.ClassPathResource;
import net.microfalx.resource.Resource;

import java.io.IOException;

public abstract class AbstractFragmentBuilder {

    private static final boolean GENERATE_FAILURES = false;

    protected final SessionMetrics createSingleModuleProject() throws IOException {
        Resource file = ClassPathResource.file("model/jvm.metrics");
        SessionMetrics session = SessionMetrics.load(file);
        generateFailures("jvm", session);
        return session;
    }

    protected final SessionMetrics createMultiModuleProject() throws IOException {
        Resource file = ClassPathResource.file("model/resource.metrics");
        SessionMetrics session = SessionMetrics.load(file);
        generateFailures("resource-core", session);
        return session;
    }

    protected final SessionMetrics createLargeMultiModuleProject() throws IOException {
        Resource file = ClassPathResource.file("model/heimdall.metrics");
        SessionMetrics session = SessionMetrics.load(file);
        generateFailures("heimdall-infrastructure-core", session);
        return session;
    }

    private void generateFailures(String module, SessionMetrics session) {
        if (!GENERATE_FAILURES) return;
        session.getModule(module).setFailureMetrics(new FailureMetrics(null, null, null, new IOException("Problem")));
        session.addExtensionFailure(new FailureMetrics(null, null, "Action", new IOException("Problem")));
    }

}
