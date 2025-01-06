package net.microfalx.maven.junit;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

@Named("surefire")
@Singleton
public class SurefireTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurefireTests.class);

    private final Map<MavenProject, Collection<ReportTestSuite>> testSuites = new ConcurrentHashMap<>();
    private int totalCount;
    private int successfulCount;
    private int failedCount;
    private int errorCount;
    private int skippedCount;

    /**
     * Returns the total number of tests across all projects.
     *
     * @return a positive integer
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Returns the total number of successful tests across all projects.
     *
     * @return a positive integer
     */
    public int getSuccessfulCount() {
        return successfulCount;
    }

    /**
     * Returns the total number of failed tests across all projects.
     *
     * @return a positive integer
     */
    public int getFailedCount() {
        return failedCount;
    }

    /**
     * Returns the total number of tests with errors across all projects.
     *
     * @return a positive integer
     */
    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Returns the total number of skipped tests across all projects.
     *
     * @return a positive integer
     */
    public int getSkippedCount() {
        return skippedCount;
    }

    /**
     * Returns all projects with tests.
     *
     * @return a non-null instance
     */
    public Collection<MavenProject> getProjects() {
        return Collections.unmodifiableCollection(testSuites.keySet());
    }

    /**
     * Returns the test suites for a project.
     *
     * @param project the project;
     * @return the tests
     */
    public Collection<ReportTestSuite> getTestSuites(MavenProject project) {
        requireNonNull(project);
        Collection<ReportTestSuite> testSuitesForProject = testSuites.get(project);
        return testSuitesForProject == null ? emptyList() : testSuitesForProject;
    }

    /**
     * Loads tests for a session.
     *
     * @param session the session
     */
    public void load(MavenSession session) {
        requireNonNull(session);
        reset();
        LOGGER.debug("Load surefire test suites");
        for (MavenProject project : session.getProjects()) {
            SurefireReportParser parser = createParser(session, project);
            if (parser == null) continue;
            List<ReportTestSuite> reportTestSuites = parser.parseXMLReportFiles();
            testSuites.put(project, reportTestSuites);
            updateStat(project, reportTestSuites);
        }
        LOGGER.debug("Loaded {} projects with tests", testSuites.size());
    }

    private SurefireReportParser createParser(MavenSession session, MavenProject project) {
        File directory = new File(new File(project.getBuild().getDirectory()), "surefire-reports");
        LOGGER.debug("Load tests for project {} from {}", project.getName(), directory);
        if (!directory.exists()) return null;
        return new SurefireReportParser(List.of(directory), new NullConsoleLogger());
    }

    private void updateStat(MavenProject project, List<ReportTestSuite> suites) {
        for (ReportTestSuite suite : suites) {
            LOGGER.debug("{} / {} = {}", project.getName(), suite.getName(), suite.getNumberOfTests());
            totalCount += suite.getNumberOfTests();
            failedCount += suite.getNumberOfFailures();
            skippedCount += suite.getNumberOfSkipped();
            errorCount += suite.getNumberOfErrors();
            successfulCount += suite.getNumberOfTests() - suite.getNumberOfErrors() - suite.getNumberOfFailures();
        }
    }

    private void reset() {
        testSuites.clear();
        totalCount = 0;
        failedCount = 0;
        skippedCount = 0;
        errorCount = 0;
        successfulCount = 0;
    }
}
