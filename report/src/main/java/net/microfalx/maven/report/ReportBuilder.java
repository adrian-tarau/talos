package net.microfalx.maven.report;

import net.microfalx.maven.model.SessionMetrics;

import java.io.Writer;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Builds an HTML report out of metrics of a Maven session.
 */
public class ReportBuilder {

    private final SessionMetrics session;

    public static ReportBuilder create(SessionMetrics session) {
        return new ReportBuilder(session);
    }

    private ReportBuilder(SessionMetrics session) {
        this.session = session;
    }

    public void build(Writer writer) {
        requireNonNull(writer);
    }
}
