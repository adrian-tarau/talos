package net.microfalx.maven.extension;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.lang.UriUtils;
import net.microfalx.maven.core.MavenLogger;
import net.microfalx.maven.core.MavenTracker;
import net.microfalx.maven.core.MavenUtils;
import net.microfalx.maven.junit.SurefireTests;
import net.microfalx.maven.model.SessionMetrics;
import net.microfalx.maven.model.TestMetrics;
import net.microfalx.maven.report.ReportBuilder;
import net.microfalx.resource.Resource;
import net.microfalx.resource.ResourceUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static net.microfalx.lang.ExceptionUtils.getRootCauseMessage;
import static net.microfalx.lang.UriUtils.parseUri;
import static net.microfalx.maven.core.MavenUtils.METRICS;

@Named("microfalx")
@Singleton
@Priority(100)
public class ProfilerLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerLifecycleParticipant.class);

    private MavenConfiguration configuration;
    private ProgressListener progressListener;
    private SessionMetrics sessionMetrics;
    private ZonedDateTime startTime;

    @Inject
    private ProfilerMetrics profilerMetrics;

    @Inject
    private TransferMetrics transferMetrics;

    @Inject
    private MavenLogger mavenLogger;

    @Inject
    private ProfilerMojoExecutionListener mojoExecutionListener;

    @Inject
    private SurefireTests tests;

    private final MavenTracker tracker = new MavenTracker(ProfilerLifecycleParticipant.class);
    private Resource report;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        tracker.track("Project Read", t -> {
            sessionMetrics = new SessionMetrics(session).setStartTime(startTime);
            profilerMetrics.sessionMetrics = sessionMetrics;
            if (progressListener != null) progressListener.start();
        });
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        tracker.track("Session Start", t -> {
            startTime = ZonedDateTime.now();
            initialize(session);
            profilerMetrics.sessionStart();
        });
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        tracker.track("Session End", t -> {
            profilerMetrics.sessionsEnd(sessionMetrics);
            METRICS.time("Update Metrics", t2 -> updateMetrics(session));
            METRICS.time("Store Metrics", t2 -> storeMetrics());
            METRICS.time("Generate Report", t2 -> generateHtmlReports());
            profilerMetrics.print();
            printConsoleReport();
            METRICS.time("Move Results", t2 -> moveResults());
            openHtmlReport();
        });
    }

    private void initialize(MavenSession session) {
        configuration = new MavenConfiguration(session);
        LOGGER.info("Initialize performance extension, logger: {}, verbose: {}, quiet: {}, console: {}, progress: {}, performance: {}",
                MavenUtils.isMavenLoggerAvailable(), configuration.isVerbose(), configuration.isConsoleEnabled(),
                configuration.isQuiet(), configuration.isProgress(), configuration.isPerformanceEnabled()
        );
        tracker.track("Register Listeners", t -> {
            registerListeners(session);
        });
        tracker.track("Start JVM Tracking", t -> {
            VirtualMachineMetrics.get().start();
        });
        tracker.track("Start Server Tracking", t -> {
            ServerMetrics.get().start();
        });
    }

    private void registerListeners(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        ChainedListener lifecycleListener = new ChainedListener(request.getExecutionListener());
        request.setExecutionListener(lifecycleListener);
        // intercepts the activity with the repository
        request.setTransferListener(transferMetrics.intercept(request.getTransferListener()));
        RepositorySystemSession repositorySession = session.getRepositorySession();
        if (repositorySession instanceof DefaultRepositorySystemSession) {
            ((DefaultRepositorySystemSession) repositorySession).setTransferListener(transferMetrics);
        }
        // intercepts lifecycle of Maven for performance metrics
        lifecycleListener.addChainListener(new ProfilerExecutionListener(profilerMetrics));
        // displays build progress
        progressListener = new ProgressListener(session, mavenLogger.getSystemOutputPrintStream());
        lifecycleListener.addChainListener(progressListener);
    }

    private void printConsoleReport() {
        if (configuration.isQuiet()) {
            mavenLogger.getSystemOutputPrintStream().println(mavenLogger.getReport());
        }
    }

    private void storeMetrics() {
        sessionMetrics.setEndTime(ZonedDateTime.now());
        try {
            sessionMetrics.setLogs(mavenLogger.getSystemOutput().loadAsString());
        } catch (IOException e) {
            LOGGER.error("Failed to attach log", e);
        }
        Resource resource = configuration.getStorageDirectory().resolve("build.data", Resource.Type.FILE);
        try {
            try (OutputStream outputStream = resource.getOutputStream()) {
                sessionMetrics.store(outputStream);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to store metrics on disk", e);
        }
    }

    private void updateMetrics(MavenSession session) {
        updateTests(session);
        updateJvm(session);
        updateRepositories(session);
    }

    private void updateJvm(MavenSession session) {
        sessionMetrics.setJvm(VirtualMachineMetrics.get().getStore());
        sessionMetrics.setServer(ServerMetrics.get().getStore());
    }

    private void updateTests(MavenSession session) {
        tests.load(session);
        Collection<TestMetrics> testMetrics = new ArrayList<>();
        for (MavenProject project : tests.getProjects()) {
            Collection<ReportTestSuite> testSuites = tests.getTestSuites(project);
            for (ReportTestSuite testSuite : testSuites) {
                for (ReportTestCase testCase : testSuite.getTestCases()) {
                    testMetrics.add(getTestMetrics(project, testCase));
                }
            }
        }
        sessionMetrics.setTests(testMetrics);
    }

    private void updateRepositories(MavenSession session) {
        sessionMetrics.setLocalRepository(parseUri(session.getLocalRepository().getBasedir()));
        sessionMetrics.setRemoteRepositories(session.getRequest().getRemoteRepositories().stream().map(ArtifactRepository::getUrl)
                .map(UriUtils::parseUri).collect(Collectors.toList()));
    }

    private TestMetrics getTestMetrics(MavenProject project, ReportTestCase testCase) {
        return new TestMetrics(project.getArtifactId(), testCase.getFullClassName(), testCase.getName())
                .setFailureErrorLine(testCase.getFailureErrorLine())
                .setFailureMessage(testCase.getFailureMessage()).setFailureType(testCase.getFailureType())
                .setFailure(testCase.hasFailure()).setError(testCase.hasError()).setSkipped(testCase.hasSkipped())
                .setFailureDetail(testCase.getFailureDetail()).setTime(testCase.getTime());
    }

    private void openHtmlReport() {
        if (configuration.isOpenReportEnabled()) {
            File file = ResourceUtils.toFile(this.report);
            try {
                Desktop.getDesktop().open(file);
            } catch (UnsupportedOperationException e) {
                // ignore the request
            } catch (IOException e) {
                System.out.println("Failed to open " + file.getAbsolutePath());
            }
        }
    }

    private void generateHtmlReports() {
        Resource resource = configuration.getStorageDirectory().resolve("build.report.html");
        try {
            ReportBuilder.create(sessionMetrics).build(resource);
        } catch (Exception e) {
            LOGGER.error("Failed to generate build report", e);
        }
        this.report = configuration.getTargetFile("build.report.html", true);
        if (configuration.isConsoleEnabled()) {
            mavenLogger.info("");
            mavenLogger.info("HTML report available at " + ResourceUtils.toFile(this.report).getAbsolutePath());
        }
    }

    private void moveResults() {
        File target = ResourceUtils.toFile(configuration.getTargetDirectory(null, true));
        File source = ResourceUtils.toFile(configuration.getStorageDirectory());
        try {
            FileUtils.copyDirectory(source, target);
        } catch (IOException e) {
            LOGGER.error("Failed to move results, root cause: {}", getRootCauseMessage(e));
        }
        try {
            FileUtils.deleteDirectory(source);
        } catch (IOException e) {
            LOGGER.error("Failed to remove temporary directory {}, root cause: {}", source, getRootCauseMessage(e));
        }
    }


}
