package net.microfalx.maven.extension;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.jvm.model.Os;
import net.microfalx.jvm.model.Server;
import net.microfalx.jvm.model.VirtualMachine;
import net.microfalx.lang.*;
import net.microfalx.maven.junit.SurefireTests;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FormatterUtils.*;
import static net.microfalx.lang.StringUtils.COMMA_WITH_SPACE;
import static net.microfalx.lang.StringUtils.isNotEmpty;
import static net.microfalx.maven.extension.MavenUtils.formatDuration;
import static net.microfalx.maven.extension.MavenUtils.*;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Collects summaries about Maven execution.
 */
@Named
@Singleton
public class ProfilerMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerMetrics.class);

    private static final int LINE_LENGTH = 110;

    private final Map<Class<?>, MojoMetrics> mojoMetrics = new ConcurrentHashMap<>();
    private final Map<String, DependencyMetrics> dependencyMetrics = new ConcurrentHashMap<>();
    private final Map<String, PluginMetrics> pluginMetrics = new ConcurrentHashMap<>();
    private final long startTime = System.nanoTime();
    private long sessionStartTime;
    private long sessionEndTime;

    @Inject
    protected MavenSession session;

    @Inject
    protected RepositoryMetrics repositoryMetrics;

    @Inject
    protected SurefireTests tests;

    private MavenConfiguration configuration;

    void sessionStart() {
        configuration = new MavenConfiguration(session);
        LOGGER.info("Initialize performance collectors, verbose: {}, minimum duration: {}",
                configuration.isVerbose(), FormatterUtils.formatDuration(configuration.getMinimumDuration()));
        sessionStartTime = System.nanoTime();
    }

    void sessionsEnd() {
        sessionEndTime = System.nanoTime();
    }

    void projectStart(MavenProject project) {
        configuration = new MavenConfiguration(session);
        registerDependencies(project);
    }

    void projectStop(MavenProject project, Throwable throwable) {

    }

    void mojoStarted(Mojo mojo, MojoExecution execution) {
        requireNonNull(mojo);
        getMetrics(mojo).start(execution);
        getMetrics(execution.getPlugin()).registerGoal(execution.getGoal());
    }

    void mojoStop(Mojo mojo, Throwable throwable) {
        requireNonNull(mojo);
        getMetrics(mojo).stop(throwable);
    }

    Duration getConfigurationDuration() {
        return MavenUtils.getDuration(startTime, sessionStartTime);
    }

    Duration getSessionDuration() {
        return MavenUtils.getDuration(sessionStartTime, sessionEndTime);
    }

    public void print() {
        if (!LOGGER.isInfoEnabled()) return;
        LOGGER.info("");
        infoLine('-');
        LOGGER.info(buffer().strong("Build Report for "
                                    + session.getTopLevelProject().getName() + " "
                                    + session.getTopLevelProject().getVersion()).toString());
        printSummary();
        printTaskSummary();
        printDependencySummary();
        printPluginSummary();
        printRepositorySummary();
        printTestsSummary();
        printInfrastructureSummary();
        infoLine('-');
    }

    private void printSummary() {
        LOGGER.info("");
        logNameValue("Request", getRequestInfo(), true, SHORT_NAME_LENGTH);
        logNameValue("Repositories", getRepositoriesInfo(), true, SHORT_NAME_LENGTH);
        if (!session.getRequest().getData().isEmpty()) logNameValue("Data", getData(), true, SHORT_NAME_LENGTH);
        LOGGER.info("");
        logNameValue("Session", formatDuration(getSessionDuration()), true, SHORT_NAME_LENGTH);
        logNameValue("Configuration", formatDuration(getConfigurationDuration()), true, SHORT_NAME_LENGTH);
        logNameValue("Repository", getRepositoryReport(), true, SHORT_NAME_LENGTH);
    }

    private void printDependencySummary() {
        Map<String, Collection<DependencyMetrics>> dependencyMetricsByGroup = getDependencyMetricsByGroup();
        LOGGER.info("");
        infoMain("Dependencies (" + dependencyMetrics.size() + " direct dependencies from " + dependencyMetricsByGroup.size()
                 + " groups and across " + session.getProjects().size() + " modules):");
        LOGGER.info("");
        for (Map.Entry<String, Collection<DependencyMetrics>> entry : dependencyMetricsByGroup.entrySet()) {
            Collection<DependencyMetrics> metrics = entry.getValue();
            String name = entry.getKey() + " (" + String.format("%1$d", metrics.size()) + ")";
            String value = "[Modules:" + String.format("%1$2d", getProjects(metrics))
                           + ", Size: " + String.format("%1$8s", formatBytes(getSize(metrics))) + "]";
            logNameValue(name, value);
        }
    }

    private void printPluginSummary() {
        LOGGER.info("");
        infoMain("Plugins (" + pluginMetrics.size() + " and across " + session.getProjects().size() + " modules):");
        LOGGER.info("");
        for (Map.Entry<String, PluginMetrics> entry : pluginMetrics.entrySet()) {
            PluginMetrics metrics = entry.getValue();
            String value = "[Modules:" + String.format("%1$2d", metrics.getProjects().size())
                           + ", Goals: '" + String.join(", ", metrics.getGoals()) + "']";
            logNameValue(metrics.getName(), value, true, LONG_NAME_LENGTH);
        }
    }

    private void printRepositorySummary() {
        if (!configuration.isVerbose() && repositoryMetrics.getArtifactResolutionDuration().compareTo(configuration.getMinimumDuration()) < 0) {
            return;
        }
        Duration minimumDuration = configuration.getMinimumDuration().dividedBy(10);
        String details = StringUtils.EMPTY_STRING;
        if (!configuration.isVerbose()) details += "limited";
        if (isNotEmpty(details)) details = " (" + details + ")";
        LOGGER.info("");
        infoMain("Repository" + details + ":");
        LOGGER.info("");
        Map<String, Collection<ArtifactMetrics>> artifactMetricsByGroup = repositoryMetrics.getArtifactMetricsByGroup();
        for (Map.Entry<String, Collection<ArtifactMetrics>> entry : artifactMetricsByGroup.entrySet()) {
            Collection<ArtifactMetrics> metrics = entry.getValue();
            Duration metadataResolveDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getMetadataResolveDuration));
            Duration metadataDownloadDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getMetadataDownloadDuration));
            Duration artifactResolveDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getArtifactResolveDuration));
            Duration artifactInstallDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getArtifactInstallDuration));
            Duration artifactDeployDuration = TimeUtils.sum(metrics.stream().map(ArtifactMetrics::getArtifactDeployDuration));
            Duration totalDuration = TimeUtils.sum(artifactResolveDuration, metadataResolveDuration, metadataDownloadDuration, artifactDeployDuration);
            if (!configuration.isVerbose() && totalDuration.compareTo(minimumDuration) < 0) continue;
            String name = entry.getKey() + " (" + String.format("%1$2d", metrics.size()) + ")";
            String value = "[Metadata: " + formatDurations(metadataResolveDuration, metadataDownloadDuration) + ", " +
                           "Artifact: " + formatDurations(artifactResolveDuration, artifactInstallDuration, artifactDeployDuration) + "]";

            logNameValue(name, value);
        }
    }

    private void printInfrastructureSummary() {
        LOGGER.info("");
        infoMain("Infrastructure:");
        LOGGER.info("");
        VirtualMachineMetrics virtualMachineMetrics = VirtualMachineMetrics.get();
        ServerMetrics serverMetrics = ServerMetrics.get();
        VirtualMachine virtualMachine = virtualMachineMetrics.getLast();
        Server server = serverMetrics.getLast();
        Os os = server.getOs();
        logNameValue("Operating system", os.getName() + " " + os.getVersion());
        logNameValue("User Name", JvmUtils.getUserName());
        logNameValue("Server Hostname", server.getHostName());
        logNameValue("Server CPU", formatPercent(serverMetrics.getAverageCpu()) + " (load: " +
                                   formatNumber(serverMetrics.getAverageLoad()) + ", cores " + server.getCores() + ")");
        logNameValue("Server Memory", formatBytes(server.getMemoryActuallyUsed()) + " of "
                                      + formatBytes(server.getMemoryTotal()));
        logNameValue("Java", virtualMachine.getName());
        logNameValue("Java CPU", formatPercent(virtualMachineMetrics.getAverageCpu()));
        logNameValue("Java Memory", formatMemory(virtualMachineMetrics.getMemoryAverage(), virtualMachineMetrics.getMemoryMaximum()));
    }

    private void printTestsSummary() {
        tests.load(session);
        if (tests.getTotalCount() == 0) return;
        String totals = getTestsReport(tests.getTotalCount(), tests.getFailedCount(),
                tests.getErrorCount(), tests.getSkippedCount());
        LOGGER.info("");
        infoMain("Tests " + totals + ":");
        LOGGER.info("");
        for (MavenProject project : tests.getProjects()) {
            Collection<ReportTestSuite> testSuites = tests.getTestSuites(project);
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(project.getName());
            buffer.append(' ');
            MavenUtils.appendDots(buffer);
            buffer.append(getTestsReport(getSum(testSuites, ReportTestSuite::getNumberOfTests),
                    getSum(testSuites, ReportTestSuite::getNumberOfFailures),
                    getSum(testSuites, ReportTestSuite::getNumberOfErrors),
                    getSum(testSuites, ReportTestSuite::getNumberOfSkipped)));
            LOGGER.info(buffer.toString());
        }
        LOGGER.info("");
    }

    public void printTaskSummary() {
        LOGGER.info("");
        infoMain("Tasks:");
        LOGGER.info("");
        for (MojoMetrics metric : getMojoMetrics()) {
            if (metric.getDuration().toMillis() == 0) continue;
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(metric.getName()).append(" (").append(metric.getGoal()).append(")");
            buffer.append(' ');
            MavenUtils.appendDots(buffer);
            buffer.append(buffer().strong(formatDuration(metric.getDuration())));
            buffer.append(' ');
            buffer.append("(");
            if (metric.getFailureCount() > 0) {
                buffer.append(buffer().warning("FAILED, " + metric.getFailureCount() + " failures"));
            } else {
                buffer.append(buffer().success("SUCCESS"));
            }
            buffer.append(", ").append(buffer().strong("Executions " + metric.getExecutionCount()));
            buffer.append(")");
            LOGGER.info(buffer.toString());
        }
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
        LOGGER.info(printNameValue(name, value));
    }

    private void logNameValue(String name, String value, boolean highlight) {
        LOGGER.info(printNameValue(name, value, highlight));
    }

    private void logNameValue(String name, String value, boolean highlight, int length) {
        LOGGER.info(printNameValue(name, value, highlight, length));
    }

    private String printNameValue(String name, String value) {
        return printNameValue(name, value, true);
    }

    private String printNameValue(String name, String value, boolean highlight) {
        return printNameValue(name, value, highlight, LONG_NAME_LENGTH);
    }

    private String printNameValue(String name, String value, boolean highlight, int length) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(name);
        buffer.append(' ');
        MavenUtils.appendDots(buffer, length);
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

    private String getRepositoryReport() {
        StringBuilder builder = new StringBuilder();
        builder.append(buffer().strong(formatDuration(repositoryMetrics.getArtifactResolutionDuration())));
        builder.append(" (Metadata: ").append(formatDurations(repositoryMetrics.getMetadataResolvedDuration(), repositoryMetrics.getMetadataDownloadDuration()))
                .append(", Artifact: ").append(formatDurations(repositoryMetrics.getArtifactResolveDuration(), repositoryMetrics.getArtifactInstallDuration(),
                        repositoryMetrics.getArtifactDeployDuration()))
                .append(buffer().strong(")"));
        return builder.toString();
    }

    private String getRequestInfo() {
        StringBuilder builder = new StringBuilder();
        String profiles = getProfiles();
        if (isNotEmpty(profiles)) StringUtils.append(builder, "Profiles: " + profiles, COMMA_WITH_SPACE);
        String goals = getGoals();
        if (isNotEmpty(goals)) StringUtils.append(builder, "Goals: " + goals, COMMA_WITH_SPACE);
        if (session.getRequest().getDegreeOfConcurrency() > 0) {
            StringUtils.append(builder, "DOP: " + session.getRequest().getDegreeOfConcurrency(), COMMA_WITH_SPACE);
        }
        if (session.getRequest().isOffline()) StringUtils.append(builder, "Offline");
        return builder.toString();
    }

    private String getGoals() {
        return String.join(" ", session.getRequest().getGoals());
    }

    private String getProfiles() {
        return String.join(" ", session.getRequest().getActiveProfiles());
    }

    private String getRepositoriesInfo() {
        return "Local: " + session.getLocalRepository().getBasedir() + ", Remote: " +
               session.getRequest().getRemoteRepositories().stream().map(ArtifactRepository::getUrl)
                       .collect(joining(", "));
    }

    private String getData() {
        Map<String, Object> data = session.getRequest().getData();
        return data.entrySet().stream().map(e -> e.getKey() + "=" + TextUtils.abbreviateMiddle(ObjectUtils.toString(e.getValue()), 10))
                .collect(joining(", "));
    }

    private String formatDurations(Duration duration1, Duration duration2) {
        if (duration1.isZero() && duration2.isZero()) {
            return String.format("%8s", ZERO_DURATION);
        } else {
            return buffer().strong(formatDuration(duration1, false, false))
                   + "/" + buffer().strong(formatDuration(duration2, false, false));
        }
    }

    private String formatDurations(Duration duration1, Duration duration2, Duration duration3) {
        if (duration1.isZero() && duration2.isZero() && duration3.isZero()) {
            return String.format("%8s", ZERO_DURATION);
        } else {
            return buffer().strong(formatDuration(duration1, false, false))
                   + "/" + buffer().strong(formatDuration(duration2, false, false))
                   + "/" + buffer().strong(formatDuration(duration3, false, false));
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
            getMetrics(dependency).register(project, dependency);
        }
        for (Plugin plugin : project.getBuildPlugins()) {
            getMetrics(plugin).register(project, plugin);
        }
    }

    private DependencyMetrics getMetrics(Dependency dependency) {
        return dependencyMetrics.computeIfAbsent(MavenUtils.getId(dependency), k -> new DependencyMetrics(dependency));
    }

    private PluginMetrics getMetrics(Plugin plugin) {
        return pluginMetrics.computeIfAbsent(MavenUtils.getId(plugin), k -> new PluginMetrics(plugin));
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
        Set<MavenProject> projects = new HashSet<>();
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
}
