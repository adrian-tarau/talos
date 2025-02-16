package net.microfalx.maven.extension;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.lang.ConcurrencyUtils;
import net.microfalx.lang.TimeUtils;
import net.microfalx.maven.core.MavenLogger;
import net.microfalx.maven.core.MavenStorage;
import net.microfalx.maven.core.MavenTracker;
import net.microfalx.maven.core.MavenUtils;
import net.microfalx.maven.junit.SurefireTests;
import net.microfalx.maven.model.*;
import net.microfalx.maven.report.ReportBuilder;
import net.microfalx.metrics.Timer;
import net.microfalx.resource.Resource;
import net.microfalx.resource.ResourceUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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

    private final CountDownLatch remoteTrendsLatch = new CountDownLatch(1);

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        tracker.track("Project Read", t -> {
            sessionMetrics = new SessionMetrics(session).setStartTime(startTime).setVerbose(configuration.isVerbose());
            profilerMetrics.sessionMetrics = sessionMetrics;
            if (progressListener != null) progressListener.start();
            startTrendsSync(session);
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
            loadProjectSettings(session);
            profilerMetrics.sessionsEnd(sessionMetrics);
            METRICS.time("Update Metrics", t2 -> updateMetrics(session));
        });
        tracker.track("Shutdown", t -> {
            METRICS.time("Collect Events", t2 -> collectExtensionEvents());
            METRICS.time("Store Metrics", t2 -> storeMetrics(session));
            METRICS.time("Generate Report", t2 -> generateHtmlReports(session));
            METRICS.time("Move Results", t2 -> copyResults(session));
            METRICS.time("Cleanup", t2 -> cleanup(session));
            profilerMetrics.print();
            printConsoleReport();
            openHtmlReport();
        });
    }

    private void initialize(MavenSession session) {
        configuration = new MavenConfiguration(session);
        LOGGER.info("Initialize performance extension, logger: {}, verbose: {}, quiet: {}, console report: {}, HTML report: {}, progress: {}, performance: {}",
                MavenUtils.isMavenLoggerAvailable(), configuration.isVerbose(), configuration.isQuiet(),
                configuration.isReportConsoleEnabled(), configuration.isReportHtmlEnabled(),
                configuration.isProgress(), configuration.isPerformanceEnabled()
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

    private void loadProjectSettings(MavenSession session) {
        MavenProject project = session.getTopLevelProject();
        if (project != null) {
            loadProjectExtensions(project);
        }
    }

    private void loadProjectExtensions(MavenProject project) {
        sessionMetrics.setExtensions(project.getBuildExtensions().stream()
                .map(ExtensionMetrics::new).collect(Collectors.toList()));
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

    private void storeMetrics(MavenSession session) {
        sessionMetrics.setEndTime(ZonedDateTime.now());
        // attach logs
        try {
            if (configuration.isReportLogsEnabled()) {
                sessionMetrics.setLogs(mavenLogger.getSystemOutput().loadAsString());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to attach log", e);
        }
        // store trend metrics
        try {
            Resource resource = MavenStorage.getStagingDirectory(session).resolve("trend.data", Resource.Type.FILE);
            try (OutputStream outputStream = resource.getOutputStream()) {
                TrendMetrics trendMetrics = TrendMetrics.from(sessionMetrics);
                trendMetrics.store(outputStream);
            }
            Resource finalResource = MavenStorage.storeTrend(session, resource);
            // upload trend metrics
            upload(() -> {
                MavenStorage.uploadTrend(session, finalResource);
                return null;
            });
        } catch (Exception e) {
            LOGGER.error("Failed to store metrics on disk", e);
        }

        // attach all trend metrics to session
        Collection<TrendMetrics> trends = getTrends(session);
        if (trends != null) sessionMetrics.setTrends(trends);
        // store session metrics
        try {
            Resource resource = MavenStorage.getStagingDirectory(session).resolve("build.data", Resource.Type.FILE);
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
        sessionMetrics.setVirtualMachineMetrics(VirtualMachineMetrics.get().getStore());
        sessionMetrics.setVirtualMachine(VirtualMachineMetrics.get().getLast());
        sessionMetrics.setServerMetrics(ServerMetrics.get().getStore());
        sessionMetrics.setServer(ServerMetrics.get().getLast());
        updateSystemProperties();
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

    private void updateSystemProperties() {
        Map<String, String> systemProperties = new HashMap<>();
        System.getProperties().forEach((k, v) -> {
            String key = k.toString();
            String value = v.toString();
            systemProperties.put(key, MavenUtils.maskSecret(key, value));
        });
        sessionMetrics.setSystemProperties(systemProperties);
    }

    private void updateRepositories(MavenSession session) {
        sessionMetrics.setLocalRepository(parseUri(session.getLocalRepository().getBasedir()));
        sessionMetrics.setRemoteRepositories(MavenUtils.getRemoteRepositories(session));
    }

    private TestMetrics getTestMetrics(MavenProject project, ReportTestCase testCase) {
        return new TestMetrics(project.getArtifactId(), testCase.getFullClassName(), testCase.getName())
                .setFailureErrorLine(testCase.getFailureErrorLine())
                .setFailureMessage(testCase.getFailureMessage()).setFailureType(testCase.getFailureType())
                .setFailure(testCase.hasFailure()).setError(testCase.hasError()).setSkipped(testCase.hasSkipped())
                .setFailureDetail(testCase.getFailureDetail()).setTime(testCase.getTime());
    }

    private void openHtmlReport() {
        if (configuration.isReportHtmlEnabled() && configuration.isOpenReportEnabled()) {
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

    private void generateHtmlReports(MavenSession session) {
        if (!configuration.isReportHtmlEnabled()) return;
        Resource resource = MavenStorage.getStagingDirectory(session).resolve("build.report.html");
        try {
            ReportBuilder.create(sessionMetrics).build(resource);
        } catch (Exception e) {
            LOGGER.error("Failed to generate build report", e);
        }
        this.report = configuration.getTargetFile("build.report.html", true);
        if (configuration.isReportConsoleEnabled() && configuration.isVerbose()) {
            mavenLogger.info("");
            mavenLogger.info("The HTML report available is at " + ResourceUtils.toFile(this.report).getAbsolutePath());
        }
    }

    private void copyResults(MavenSession session) {
        // copy results from staging to local sessions
        File sessionTarget = ResourceUtils.toFile(MavenStorage.getLocalSessionsDirectory(session));
        copyResults(session, sessionTarget, false);
        // upload results to remote store
        upload(() -> {
            MavenStorage.getRemoteSessionsDirectory(session).copyFrom(Resource.directory(sessionTarget));
            return null;
        });
        // copy results in $ROOT/target directory
        File projectTarget = ResourceUtils.toFile(configuration.getTargetDirectory(null, true));
        copyResults(session, projectTarget, true);
    }

    private void collectExtensionEvents() {
        Collection<LifecycleMetrics> extensionEvents = new ArrayList<>();
        for (Timer timer : METRICS.getTimers()) {
            if (timer.getCount() == 0) continue;
            extensionEvents.add(new LifecycleMetrics(timer.getName()).addActiveDuration(timer.getDuration(), (int) timer.getCount()));
        }
        sessionMetrics.setExtensionsEvents(extensionEvents);
    }

    private void copyResults(MavenSession session, File target, boolean remove) {
        File source = ResourceUtils.toFile(MavenStorage.getStagingDirectory(session));
        try {
            FileUtils.copyDirectory(source, target);
        } catch (IOException e) {
            LOGGER.error("Failed to copy results to {}, root cause: {}", target, getRootCauseMessage(e));
        }
        if (remove) {
            try {
                FileUtils.deleteDirectory(source);
            } catch (IOException e) {
                LOGGER.error("Failed to remove source {}, root cause: {}", source, getRootCauseMessage(e));
            }
        }
    }

    private Collection<TrendMetrics> getTrends(MavenSession session) {
        return tracker.trackCallable("Load Trends", () -> {
            ConcurrencyUtils.await(remoteTrendsLatch);
            boolean trendReportingDaily = configuration.isTrendReportingDaily();
            LocalDateTime oldestTrend = LocalDateTime.now().minus(configuration.getTrendRetention());
            Collection<TrendMetrics> metrics = new ArrayList<>();
            Collection<Resource> resources = MavenStorage.getLocalTrends(session);
            LocalDate prevDate = null;
            for (Resource resource : resources) {
                LocalDateTime lastModified = TimeUtils.toLocalDateTime(resource.lastModified());
                LocalDate date = lastModified.toLocalDate();
                if (lastModified.isAfter(oldestTrend)) {
                    if (!trendReportingDaily || (prevDate == null || !prevDate.equals(date))) {
                        metrics.add(loadTrend(resource));
                    }
                    prevDate = date;
                } else {
                    try {
                        resource.delete();
                    } catch (IOException e) {
                        // it does not matter, after some time it will be successful
                    }
                }
            }
            return metrics;
        });
    }

    private Collection<Resource> getRemoteTrends(MavenSession session) {
        LocalDateTime oldestTrend = LocalDateTime.now().minus(configuration.getTrendRetention());
        Collection<Resource> finalTrends = new ArrayList<>();
        try {
            Collection<Resource> remoteTrends = MavenStorage.getRemoteTrends(session);
            for (Resource remoteTrend : remoteTrends) {
                LocalDateTime lastModified = TimeUtils.toLocalDateTime(remoteTrend.lastModified());
                if (lastModified.isAfter(oldestTrend)) {
                    finalTrends.add(remoteTrend);
                } else {
                    try {
                        remoteTrend.delete();
                    } catch (IOException e) {
                        // it does not matter, after some time it will be successful
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to extract remote trends, root cause: {}", getRootCauseMessage(e));
        }
        return finalTrends;
    }

    private void copyRemoteTrendsLocally(MavenSession session) {
        try {
            Resource localTrendsDirectory = MavenStorage.getLocalTrendsDirectory(session);
            Collection<Resource> remoteTrends = getRemoteTrends(session);
            int successCount = 0;
            int failureCount = 0;
            for (Resource remoteTrend : remoteTrends) {
                Resource localTrend = localTrendsDirectory.resolve(remoteTrend.getFileName(), Resource.Type.FILE);
                try {
                    if (!ResourceUtils.hasSameAttributes(localTrend, remoteTrend, false)) {
                        localTrend.copyFrom(remoteTrend);
                    }
                    successCount++;
                } catch (IOException e) {
                    failureCount++;
                    LOGGER.warn("Failed to copy remote trend '{}', root cause: {}", remoteTrend, getRootCauseMessage(e));
                }
            }
            LOGGER.info("Synchronized successfuly {} trends, unsuccessfully {}", successCount, failureCount);
        } finally {
            remoteTrendsLatch.countDown();
        }
    }

    private void startTrendsSync(MavenSession session) {
        if (!session.getResult().hasExceptions()) {
            Thread thread = new Thread(new CopyRemoteTrendsTask(session));
            thread.setName("Sync Trends");
            thread.start();
        }
    }

    private void upload(Callable<?> callable) {
        tracker.trackCallable("Upload", callable);
    }

    private TrendMetrics loadTrend(Resource resource) {
        return tracker.trackCallable("Load Trend", () -> TrendMetrics.load(resource));
    }

    private void cleanup(MavenSession session) {
        MavenStorage.cleanupWorkspace(session);
    }

    class CopyRemoteTrendsTask implements Runnable {

        private final MavenSession session;

        public CopyRemoteTrendsTask(MavenSession session) {
            this.session = session;
        }

        @Override
        public void run() {
            copyRemoteTrendsLocally(session);
        }
    }


}
