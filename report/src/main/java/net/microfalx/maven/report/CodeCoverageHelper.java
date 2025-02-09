package net.microfalx.maven.report;

import net.microfalx.maven.model.SessionMetrics;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class CodeCoverageHelper {

    private final SessionMetrics session;
    private final ReportHelper reportHelper;

    public CodeCoverageHelper(SessionMetrics session, ReportHelper reportHelper) {
        requireNonNull(session);
        requireNonNull(reportHelper);
        this.session = session;
        this.reportHelper = reportHelper;
    }

    public boolean hasCoverage() {
        return false;
    }
}
