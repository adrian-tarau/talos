package net.microfalx.maven.extension;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.jvm.model.Os;
import net.microfalx.jvm.model.Server;
import net.microfalx.jvm.model.VirtualMachine;
import net.microfalx.lang.*;
import net.microfalx.maven.core.MavenLogger;
import net.microfalx.maven.core.MavenTracker;
import net.microfalx.maven.junit.SurefireTests;
import net.microfalx.maven.model.*;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.internal.DefaultDependencyGraphBuilder;
import org.eclipse.aether.RepositorySystem;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.*;
import static net.microfalx.lang.FormatterUtils.formatNumber;
import static net.microfalx.lang.FormatterUtils.formatPercent;
import static net.microfalx.lang.StringUtils.isNotEmpty;
import static net.microfalx.lang.TextUtils.insertSpaces;
import static net.microfalx.maven.core.MavenUtils.*;
import static net.microfalx.maven.extension.MavenUtils.*;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Collects summaries about Maven execution.
 */
@Named
@Singleton
public class ProfilerMetrics {

    private static final MavenLogger LOGGER = MavenLogger.create(ProfilerMetrics.class);

    private static final int LINE_LENGTH = 110;

    private final Map<Class<?>, MojoMetrics> mojoMetrics = new ConcurrentHashMap<>();
    private final Map<String, DependencyMetrics> dependencyMetrics = new ConcurrentHashMap<>();
    private final Map<String, PluginMetrics> pluginMetrics = new ConcurrentHashMap<>();
    private final Map<String, ProjectMetrics> projectMetrics = new ConcurrentHashMap<>();
    private final long startTime = System.nanoTime();
    private long sessionStartTime;
    private long sessionEndTime;

    @Inject
    protected MavenSession session;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected ProjectDependenciesResolver dependenciesResolver;

    @Inject
    protected MavenLogger logger;

    @Inject
    protected RepositoryMetrics repositoryMetrics;

    @Inject
    protected TransferMetrics transferMetrics;

    @Inject
    protected SurefireTests tests;

    private final MavenTracker tracker = new MavenTracker(ProfilerMetrics.class);
    private MavenConfiguration configuration;
    SessionMetrics sessionMetrics;

    void sessionStart() {
        configuration = new MavenConfiguration(session);
        LOGGER.debug("Initialize performance collectors, minimum duration: {}",
                FormatterUtils.formatDuration(configuration.getMinimumDuration()));
        sessionStartTime = System.nanoTime();
    }

    void sessionsEnd(SessionMetrics sessionMetrics) {
        updateLifeCycle(sessionMetrics);
        sessionMetrics.setEndTime(ZonedDateTime.now());
        sessionMetrics.setArtifacts(repositoryMetrics.getMetrics());
        sessionMetrics.setDependencies(dependencyMetrics.values());
        sessionMetrics.setMojos(mojoMetrics.values());
        sessionMetrics.setPlugins(pluginMetrics.values());
        tracker.track("Update Dependencies", t -> updateDependencies());
        tracker.track("Record Failures", t -> {
            sessionMetrics.setExtensionFailures(MavenTracker.getFailures().stream()
                    .map(f -> new FailureMetrics(f.getProject(), f.getMojo(), f.getName(), f.getThrowable()))
                    .collect(Collectors.toList()));
        });
        sessionEndTime = System.nanoTime();
    }

    void projectStart(MavenProject project) {
        tracker.track("Project Start", t -> {
            sessionMetrics.addModule(getMetrics(project).setStartTime(ZonedDateTime.now()));
            configuration = new MavenConfiguration(session);
            registerDependencies(project);
        }, project);
    }

    void projectStop(MavenProject project, Throwable throwable) {
        tracker.track("Project Stop", t -> {
            ProjectMetrics projectMetrics = getMetrics(project);
            projectMetrics.setEndTime(ZonedDateTime.now());
            if (throwable != null) {
                projectMetrics.setFailureMetrics(new FailureMetrics(project, null, "Project Stop", throwable));
            }
        });
    }

    void mojoStarted(Mojo mojo, MojoExecution execution) {
        requireNonNull(mojo);
        getMetrics(mojo).start(execution);
        getMetrics(execution.getPlugin()).registerGoal(execution.getGoal());
    }

    void mojoStop(MavenProject project, Mojo mojo, Throwable throwable) {
        requireNonNull(mojo);
        getMetrics(mojo).stop(project, throwable);
    }

    Duration getConfigurationDuration() {
        return MavenUtils.getDuration(startTime, sessionStartTime);
    }

    Duration getSessionDuration() {
        return MavenUtils.getDuration(sessionStartTime, sessionEndTime);
    }

    void print() {
        if (!configuration.isReportConsoleEnabled()) {
            logger.getSystemOutputPrintStream().println();
            return;
        }
        LOGGER.info("");
        if (shouldShowLineSeparator()) infoLine('-');
        if (configuration.isQuiet()) LOGGER.info("");
        LOGGER.info(buffer().strong("Build Report for "
                                    + session.getTopLevelProject().getName() + " "
                                    + session.getTopLevelProject().getVersion()).toString());
        printSummary();
        printTaskSummary();
        printDependencySummary();
        printPluginSummary();
        printRepositorySummary();
        printTestsSummary();
        if (configuration.isEnvironmentEnabled() || configuration.isVerbose()) printEnvironmentSummary();
        printFailureSummary();
        if (shouldShowLineSeparator()) infoLine('-');
        if (configuration.isQuiet() && net.microfalx.maven.core.MavenUtils.isMavenLoggerAvailable()) {
            logger.getSystemOutputPrintStream().println(LOGGER.getReport());
        }
    }

    private void printSummary() {
        LOGGER.info("");
        increaseIndent();
        logNameValue("Request", MavenUtils.getRequestInfo(session), true, SHORT_NAME_LENGTH);
        logNameValue("Repositories", getRepositoriesInfo(), true, SHORT_NAME_LENGTH);
        if (!session.getRequest().getData().isEmpty()) logNameValue("Data", getData(), true, SHORT_NAME_LENGTH);
        LOGGER.info("");
        logNameValue("Session", formatDuration(getSessionDuration()), true, SHORT_NAME_LENGTH);
        logNameValue("Configuration", formatDuration(getConfigurationDuration()), true, SHORT_NAME_LENGTH);
        logNameValue("Compile", formatDuration(getGoalsDuration(COMPILE_GOALS)), true, SHORT_NAME_LENGTH);
        logNameValue("Tests", formatDuration(getGoalsDuration(TESTS_GOALS)), true, SHORT_NAME_LENGTH);
        logNameValue("Package", formatDuration(getGoalsDuration(PACKAGE_GOALS)), true, SHORT_NAME_LENGTH);
        logNameValue("Install", formatDuration(getGoalsDuration(INSTALL_GOALS)), true, SHORT_NAME_LENGTH);
        logNameValue("Deploy", formatDuration(getGoalsDuration(DEPLOY_GOALS)), true, SHORT_NAME_LENGTH);
        if (tracker.getDuration().toMillis() > configuration.getMinimumDuration().toMillis()) {
            logNameValue("Performance", formatDuration(tracker.getDuration()), true, SHORT_NAME_LENGTH);
        }
        if (!MavenTracker.getFailures().isEmpty()) {
            logNameValue("Extension Failures", FormatterUtils.formatNumber(MavenTracker.getFailures()), true, SHORT_NAME_LENGTH);
        }
        logNameValue("Local Repository", getRepositoryReport(repositoryMetrics), true, SHORT_NAME_LENGTH);
        logNameValue("Remote Repository", getRepositoryReport(transferMetrics), true, SHORT_NAME_LENGTH);
        decreaseIndent();
    }

    private void printDependencySummary() {
        if (!configuration.isVerbose()) return;
        Map<String, Collection<DependencyMetrics>> dependencyMetricsByGroup = getDependencyMetricsByGroup();
        LOGGER.info("");
        infoMain("Dependencies (" + dependencyMetrics.size() + " direct dependencies from " + dependencyMetricsByGroup.size()
                 + " groups and across " + session.getProjects().size() + " modules):");
        LOGGER.info("");
        increaseIndent();
        for (Map.Entry<String, Collection<DependencyMetrics>> entry : dependencyMetricsByGroup.entrySet()) {
            Collection<DependencyMetrics> metrics = entry.getValue();
            String name = entry.getKey() + " (" + String.format("%1$d", metrics.size()) + ")";
            String value = "[Modules:" + String.format("%1$2d", getProjects(metrics))
                           + ", Size: " + String.format("%1$8s", formatBytes(getSize(metrics))) + "]";
            logNameValue(name, value);
        }
        decreaseIndent();
    }

    private void printPluginSummary() {
        if (!configuration.isVerbose()) return;
        LOGGER.info("");
        infoMain("Plugins (" + pluginMetrics.size() + " and across " + session.getProjects().size() + " modules):");
        LOGGER.info("");
        increaseIndent();
        for (Map.Entry<String, PluginMetrics> entry : pluginMetrics.entrySet()) {
            PluginMetrics metrics = entry.getValue();
            String value = "[Modules:" + String.format("%1$2d", metrics.getProjects().size())
                           + ", Goals: '" + String.join(", ", metrics.getGoals()) + "']";
            logNameValue(metrics.getName(), value, true, LONG_NAME_LENGTH);
        }
        decreaseIndent();
    }

    private void printRepositorySummary() {
        if (!configuration.isVerbose()) return;
        printRepositorySummary("Local Repository", repositoryMetrics);
        printRepositorySummary("Remote Repository", transferMetrics);
    }


    private void printRepositorySummary(String title, AbstractRepositoryMetrics repositoryMetrics) {
        if (repositoryMetrics.getResolutionDuration().compareTo(configuration.getMinimumDuration()) < 0) {
            return;
        }
        Duration minimumDuration = configuration.getMinimumDuration().dividedBy(10);
        String details = StringUtils.EMPTY_STRING;
        if (!configuration.isVerbose()) details += "limited";
        if (isNotEmpty(details)) details = " (" + details + ")";
        LOGGER.info("");
        infoMain(title + " " + details + ":");
        LOGGER.info("");
        increaseIndent();
        boolean remote = repositoryMetrics instanceof TransferMetrics;
        for (Map.Entry<String, Collection<ArtifactMetrics>> entry : repositoryMetrics.getMetricsByGroup().entrySet()) {
            Collection<ArtifactMetrics> metrics = entry.getValue();
            Duration metadataResolveDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getMetadataResolveDuration));
            Duration metadataDownloadDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getMetadataDownloadDuration));
            Duration artifactResolveDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getArtifactResolveDuration));
            Duration artifactInstallDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getArtifactInstallDuration));
            Duration artifactDeployDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getArtifactDeployDuration));
            Duration totalDuration = TimeUtils.sum(artifactResolveDuration, metadataResolveDuration, metadataDownloadDuration, artifactDeployDuration);
            if (!configuration.isVerbose() && totalDuration.compareTo(minimumDuration) < 0) continue;
            String name = entry.getKey() + " (" + String.format("%1$2d", metrics.size()) + ")";
            String value;
            if (remote) {
                value = "[Metadata: " + formatDuration(metadataResolveDuration) + ", " +
                        "Artifact: " + formatDuration(artifactResolveDuration) + "]";
            } else {
                value = "[Metadata: " + formatDurations(metadataResolveDuration, metadataDownloadDuration) + ", " +
                        "Artifact: " + formatDurations(artifactResolveDuration, artifactInstallDuration, artifactDeployDuration) + "]";
            }
            logNameValue(name, value);
        }
        decreaseIndent();
    }

    private void printEnvironmentSummary() {
        LOGGER.info("");
        infoMain("Environment:");
        LOGGER.info("");
        increaseIndent();
        VirtualMachineMetrics virtualMachineMetrics = VirtualMachineMetrics.get();
        ServerMetrics serverMetrics = ServerMetrics.get();
        VirtualMachine virtualMachine = virtualMachineMetrics.getLast();
        Server server = serverMetrics.getLast();
        Os os = server.getOs();
        logNameValue("Operating System", os.getName() + " " + os.getVersion());
        logNameValue("Java", virtualMachine.getName());
        logNameValue("User Name", JvmUtils.getUserName() + "@" + server.getHostName());
        logNameValue("Server", "CPU: " + formatPercent(serverMetrics.getAverageCpu()) + " (load: " +
                               formatNumber(serverMetrics.getAverageLoad()) + ", cores " + server.getCores() + ")"
                               + ", Memory: " + formatMemory(server.getMemoryActuallyUsed(), server.getMemoryTotal()));
        logNameValue("Process", "CPU: " + formatPercent(virtualMachineMetrics.getAverageCpu())
                                + ", Memory: " + formatMemory(virtualMachineMetrics.getMemoryAverage(), virtualMachineMetrics.getMemoryMaximum()));
        decreaseIndent();
    }

    private void printFailureSummary() {
        MavenExecutionResult result = session.getResult();
        if (!result.hasExceptions()) return;
        LOGGER.info("");
        infoMain(buffer().failure("Failures") + " (" + result.getExceptions().size() + ")");
        LOGGER.info("");
        for (Throwable exception : result.getExceptions()) {
            logNameValue("Exception Type", ClassUtils.getName(getRootCause(exception)));
            logNameValue("Exception Message", getRootCauseMessage(exception));
            if (session.getRequest().isShowErrors()) {
                logNameValue("Stack Trace", "\n" + insertSpaces(getStackTrace(exception), 5));
            }
        }
    }

    private void printTestsSummary() {
        tests.load(session);
        if (tests.getTotalCount() == 0) return;
        String totals = getTestsReport(tests.getTotalCount(), tests.getFailedCount(),
                tests.getErrorCount(), tests.getSkippedCount());
        LOGGER.info("");
        infoMain("Tests " + totals + ":");
        LOGGER.info("");
        increaseIndent();
        for (MavenProject project : tests.getProjects()) {
            Collection<ReportTestSuite> testSuites = tests.getTestSuites(project);
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(project.getName()).append(' ');
            MavenUtils.appendDots(buffer).append(' ');
            buffer.append(getTestsReport(getSum(testSuites, ReportTestSuite::getNumberOfTests),
                    getSum(testSuites, ReportTestSuite::getNumberOfFailures),
                    getSum(testSuites, ReportTestSuite::getNumberOfErrors),
                    getSum(testSuites, ReportTestSuite::getNumberOfSkipped)));
            LOGGER.info(getIndentSpaces() + buffer);
        }
        decreaseIndent();
        LOGGER.info("");
    }

    public void printTaskSummary() {
        LOGGER.info("");
        infoMain("Tasks:");
        LOGGER.info("");
        increaseIndent();
        for (MojoMetrics metric : getMojoMetrics()) {
            if (metric.getDuration().toMillis() == 0) continue;
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(metric.getName()).append(" (").append(metric.getGoal()).append(") ");
            MavenUtils.appendDots(buffer).append(' ');
            buffer.append(buffer().strong(formatDuration(metric.getDuration())));
            buffer.append(" (");
            if (metric.getFailureCount() > 0) {
                buffer.append(buffer().warning("FAILED, " + metric.getFailureCount() + " failures"));
            } else {
                buffer.append(buffer().success("SUCCESS"));
            }
            buffer.append(", ").append(buffer().strong("Executions " + metric.getExecutionCount()));
            buffer.append(")");
            LOGGER.info(getIndentSpaces() + buffer);
        }
        decreaseIndent();
    }

    private Collection<MojoMetrics> getMojoMetrics() {
        List<MojoMetrics> metrics = new ArrayList<>(mojoMetrics.values());
        metrics.sort(Comparator.comparing(MojoMetrics::getDuration).reversed());
        return metrics;
    }

    private MojoMetrics getMetrics(Mojo mojo) {
        return mojoMetrics.computeIfAbsent(mojo.getClass(), k -> new MojoMetrics(mojo));
    }

    private void infoLine(char c) {
        infoMain(String.valueOf(c).repeat(LINE_LENGTH));
    }

    private void infoMain(String msg) {
        LOGGER.info(buffer().strong(msg).toString());
    }

    private void logNameValue(String name, String value) {
        LOGGER.info(getIndentSpaces() + printNameValue(name, value));
    }

    private void logNameValue(String name, String value, boolean highlight) {
        LOGGER.info(getIndentSpaces() + printNameValue(name, value, highlight));
    }

    private void logNameValue(String name, String value, boolean highlight, int length) {
        LOGGER.info(getIndentSpaces() + printNameValue(name, value, highlight, length));
    }

    private String printNameValue(String name, String value) {
        return printNameValue(name, value, true);
    }

    private String printNameValue(String name, String value, boolean highlight) {
        return printNameValue(name, value, highlight, LONG_NAME_LENGTH);
    }

    private String printNameValue(String name, String value, boolean highlight, int length) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(name).append(' ');
        MavenUtils.appendDots(buffer, length).append(' ');
        buffer.append(highlight ? buffer().strong(value) : value);
        return buffer.toString();
    }

    private String getTestsReport(int totalCount, int failedCount, int errorCount, int skippedCount) {
        int totalErrors = failedCount + errorCount;
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        builder.append(buffer().strong(formatInteger(totalCount, 4))).append("/");
        builder.append(totalErrors > 0 ? buffer().failure(formatInteger(totalErrors, 3))
                : buffer().success(formatInteger(totalErrors, 3))).append("/");
        builder.append(formatInteger(skippedCount, 2));
        builder.append(']');
        return builder.toString();
    }

    private String getRepositoryReport(AbstractRepositoryMetrics repositoryMetrics) {
        StringBuilder builder = new StringBuilder();
        builder.append(buffer().strong(formatDuration(repositoryMetrics.getResolutionDuration())));
        boolean remote = repositoryMetrics instanceof TransferMetrics;
        if (remote) {
            builder.append(" (Metadata: ").append(formatDuration(repositoryMetrics.getMetadataResolvedDuration()))
                    .append(", Artifact: ").append(formatDuration(repositoryMetrics.getArtifactResolveDuration()))
                    .append(buffer().strong(")"));
            builder.append(" (Download: ").append(formatBytes(repositoryMetrics.getDownloadVolume()))
                    .append(" / Upload: ").append(formatBytes(repositoryMetrics.getUploadVolume())).append(")");
        } else {
            builder.append(" (Metadata: ").append(formatDurations(repositoryMetrics.getMetadataResolvedDuration(), repositoryMetrics.getMetadataDownloadDuration()))
                    .append(", Artifact: ").append(formatDurations(repositoryMetrics.getArtifactResolveDuration(), repositoryMetrics.getArtifactInstallDuration(),
                            repositoryMetrics.getArtifactDeployDuration()))
                    .append(buffer().strong(")"));
        }
        return builder.toString();
    }

    private Duration getRepositoryDuration(AbstractRepositoryMetrics repositoryMetrics) {
        return TimeUtils.sum(repositoryMetrics.getMetadataResolvedDuration(), repositoryMetrics.getArtifactResolveDuration());
    }

    private String getRepositoriesInfo() {
        return "Local: " + session.getLocalRepository().getBasedir() + ", Remote: " +
               net.microfalx.maven.core.MavenUtils.getRemoteRepositories(session).stream()
                       .map(URI::toASCIIString).collect(joining(", "));
    }

    private String getData() {
        Map<String, Object> data = session.getRequest().getData();
        return data.entrySet().stream().map(e -> e.getKey() + "=" + TextUtils.abbreviateMiddle(ObjectUtils.toString(e.getValue()), 10))
                .collect(joining(", "));
    }

    private Duration getGoalsDuration(String... goals) {
        Duration duration = Duration.ZERO;
        for (MojoMetrics metric : getMojoMetrics()) {
            if (StringUtils.containsInArray(metric.getGoal(), goals)) {
                duration = duration.plus(metric.getDuration());
            }
        }
        return duration;
    }

    private String formatDurations(Duration duration1, Duration duration2) {
        if (duration1.isZero() && duration2.isZero()) {
            return String.format("%8s", ZERO_DURATION);
        } else {
            return buffer().strong(formatDuration(duration1))
                   + "/" + buffer().strong(formatDuration(duration2));
        }
    }

    private String formatDuration(Duration duration) {
        return buffer().strong(net.microfalx.maven.core.MavenUtils.formatDuration(duration, false, false)).toString();
    }

    private String formatBytes(long value) {
        return buffer().strong(FormatterUtils.formatBytes(value)).toString();
    }

    private String formatDurations(Duration duration1, Duration duration2, Duration duration3) {
        if (duration1.isZero() && duration2.isZero() && duration3.isZero()) {
            return String.format("%8s", ZERO_DURATION);
        } else {
            return formatDuration(duration1)
                   + "/" + formatDuration(duration2)
                   + "/" + formatDuration(duration3);
        }
    }

    private int getSum(Collection<ReportTestSuite> testSuites, Function<ReportTestSuite, Integer> function) {
        int total = 0;
        for (ReportTestSuite testSuite : testSuites) {
            total += function.apply(testSuite);
        }
        return total;
    }

    private void registerDependencies(MavenProject project) {
        for (Dependency dependency : project.getDependencies()) {
            getMetrics(dependency).register(project, dependency).setScope(dependency.getScope()).setType(dependency.getType())
                    .setOptional(dependency.isOptional());
        }
        for (Plugin plugin : project.getBuildPlugins()) {
            getMetrics(plugin).register(project, plugin);
        }
        DependencyGraphBuilder dependencyGraphBuilder = new DefaultDependencyGraphBuilder(dependenciesResolver);
        DependencyNode node = resolveProject(dependencyGraphBuilder, project);
        if (node != null) {
            walkDependencyGraph(node, project, false);
        }
    }

    private void walkDependencyGraph(DependencyNode node, MavenProject project, boolean transitive) {
        for (DependencyNode child : node.getChildren()) {
            Dependency dependency = MavenUtils.fromArtifact(child.getArtifact());
            DependencyMetrics dependencyMetrics = getMetrics(dependency).register(project, dependency);
            dependencyMetrics.setScope(dependency.getScope()).setType(dependency.getType())
                    .setOptional(dependency.isOptional()).setTransitive(transitive);
            if (child.getArtifact().getFile() != null) {
                dependencyMetrics.setSize(child.getArtifact().getFile().length());
            }
            walkDependencyGraph(child, project, true);
        }
    }

    private DependencyNode resolveProject(DependencyGraphBuilder dependencyGraphBuilder, MavenProject project) {
        try {
            ArtifactFilter artifactFilter = artifact -> true;
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            buildingRequest.setProject(project);
            return dependencyGraphBuilder.buildDependencyGraph(buildingRequest, artifactFilter);
        } catch (DependencyGraphBuilderException e) {
            tracker.logFailure("Resolve Dependency Graph", e);
            return null;
        }
    }

    private void updateLifeCycle(SessionMetrics sessionMetrics) {
        Collection<LifecycleMetrics> lifecycles = new ArrayList<>();
        lifecycles.add(new LifecycleMetrics("Configuration").addActiveDuration(getConfigurationDuration()));
        lifecycles.add(new LifecycleMetrics("Compile").addActiveDuration(getGoalsDuration(COMPILE_GOALS)));
        lifecycles.add(new LifecycleMetrics("Tests").addActiveDuration(getGoalsDuration(TESTS_GOALS)));
        lifecycles.add(new LifecycleMetrics("Package").addActiveDuration(getGoalsDuration(PACKAGE_GOALS)));
        lifecycles.add(new LifecycleMetrics("Install").addActiveDuration(getGoalsDuration(INSTALL_GOALS)));
        lifecycles.add(new LifecycleMetrics("Deploy").addActiveDuration(getGoalsDuration(DEPLOY_GOALS)));
        lifecycles.add(new LifecycleMetrics("Local Repository").addActiveDuration(getRepositoryDuration(repositoryMetrics)));
        lifecycles.add(new LifecycleMetrics("Remote Repository").addActiveDuration(getRepositoryDuration(transferMetrics)));
        sessionMetrics.setLifeCycles(lifecycles);
    }

    private void updateDependencies() {
        for (DependencyMetrics dependencyMetrics : dependencyMetrics.values()) {
            ArtifactMetrics artifactMetrics = repositoryMetrics.get(MavenUtils.getId(dependencyMetrics));
            if (artifactMetrics != null) {
                dependencyMetrics.setSize(artifactMetrics.getSize());
                dependencyMetrics.setDuration(artifactMetrics.getDuration());
            }
        }
    }

    private DependencyMetrics getMetrics(Dependency dependency) {
        return dependencyMetrics.computeIfAbsent(net.microfalx.maven.core.MavenUtils.getId(dependency), k -> new DependencyMetrics(dependency));
    }

    private PluginMetrics getMetrics(Plugin plugin) {
        return pluginMetrics.computeIfAbsent(net.microfalx.maven.core.MavenUtils.getId(plugin), k -> new PluginMetrics(plugin));
    }

    private ProjectMetrics getMetrics(MavenProject project) {
        return projectMetrics.computeIfAbsent(net.microfalx.maven.core.MavenUtils.getId(project), k -> new ProjectMetrics(project));
    }

    private long getSize(Collection<DependencyMetrics> metrics) {
        long size = 0;
        for (DependencyMetrics metric : metrics) {
            ArtifactMetrics artifactMetrics = repositoryMetrics.get(metric.getId());
            size += artifactMetrics.getSize();
        }
        return size;
    }

    private int getProjects(Collection<DependencyMetrics> metrics) {
        Set<Project> projects = new HashSet<>();
        for (DependencyMetrics metric : metrics) {
            projects.addAll(metric.getProjects());
        }
        return projects.size();
    }

    private Map<String, Collection<DependencyMetrics>> getDependencyMetricsByGroup() {
        Map<String, Collection<DependencyMetrics>> map = new TreeMap<>();
        for (DependencyMetrics dependencyMetrics : dependencyMetrics.values()) {
            map.computeIfAbsent(dependencyMetrics.getGroupId(), s -> new ArrayList<>()).add(dependencyMetrics);
        }
        return map;
    }

    private boolean shouldShowLineSeparator() {
        return !configuration.isQuietAndWithProgress();
    }

    private static final String[] COMPILE_GOALS = {"compiler:compile", "compiler:testCompile", "resources:resources", "resources:testResources"};
    private static final String[] TESTS_GOALS = {"surefire:test", "failsafe:verify", "jacoco:report"};
    private static final String[] PACKAGE_GOALS = {"jar:jar",};
    private static final String[] INSTALL_GOALS = {"install:install"};
    private static final String[] DEPLOY_GOALS = {"install:deploy"};


}
