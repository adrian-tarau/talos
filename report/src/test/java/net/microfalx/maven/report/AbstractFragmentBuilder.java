package net.microfalx.maven.report;

import net.microfalx.maven.model.SessionMetrics;
import net.microfalx.resource.ClassPathResource;
import net.microfalx.resource.Resource;

import java.io.IOException;

public abstract class AbstractFragmentBuilder {

    protected final SessionMetrics createSingleModuleProject() throws IOException {
        Resource file = ClassPathResource.file("model/jvm.metrics");
        return SessionMetrics.load(file);
    }

    protected final SessionMetrics createMultiModuleProject() throws IOException {
        Resource file = ClassPathResource.file("model/resource.metrics");
        return SessionMetrics.load(file);
    }

}
