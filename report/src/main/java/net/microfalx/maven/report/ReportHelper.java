package net.microfalx.maven.report;

import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.TimeUtils;
import net.microfalx.maven.model.MojoMetrics;
import net.microfalx.maven.model.SessionMetrics;
import net.microfalx.resource.Resource;

import java.io.IOException;
import java.time.Duration;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class ReportHelper {

    private final SessionMetrics session;

    public ReportHelper(SessionMetrics session) {
        requireNonNull(session);
        this.session = session;
    }

    public long getFailureCount() {
        return session.getMojos().stream().mapToLong(MojoMetrics::getFailureCount).sum();
    }

    public long getExecutionCount() {
        return session.getMojos().stream().mapToLong(MojoMetrics::getExecutionCount).sum();
    }

    public String getBuildTime() {
        return TimeUtils.toString(session.getMojos().stream().map(MojoMetrics::getDuration).reduce(Duration.ZERO, Duration::plus));
    }

    public long getProjectCount() {
        return session.getModules().size();
    }

    public String getLogAsHtml() {
        AnsiToHtml ansiToHtml = new AnsiToHtml();
        try {
            Resource resource = ansiToHtml.transform(Resource.text(session.getLog()));
            return resource.loadAsString();
        } catch (IOException e) {
            return "#ERROR: " + ExceptionUtils.getRootCauseMessage(e);
        }
    }

}
