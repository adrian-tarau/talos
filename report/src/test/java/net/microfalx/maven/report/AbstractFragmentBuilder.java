package net.microfalx.maven.report;

import net.microfalx.maven.model.SessionMetrics;
import net.microfalx.resource.ClassPathResource;
import net.microfalx.resource.Resource;

import java.io.IOException;

public abstract class AbstractFragmentBuilder {

    protected final SessionMetrics createSingleModuleProject() throws IOException {
        Resource file = ClassPathResource.file("model/jvm.metrics");
        SessionMetrics session = SessionMetrics.load(file);
        return session;
    }

    protected final SessionMetrics createMultiModuleProject() throws IOException {
        Resource file = ClassPathResource.file("model/resource.metrics");
        SessionMetrics session = SessionMetrics.load(file);
        session.setThrowable(new IOException("Problem"));
        return session;
    }

}
