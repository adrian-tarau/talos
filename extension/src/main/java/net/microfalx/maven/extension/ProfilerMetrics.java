package net.microfalx.maven.extension;

import net.microfalx.jvm.model.Os;
import net.microfalx.jvm.model.Server;
import net.microfalx.jvm.model.VirtualMachine;
import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.JvmUtils;
import net.microfalx.lang.Nameable;
import net.microfalx.maven.junit.SurefireTests;
import org.apache.maven.execution.MavenSession;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.time.Duration.ofNanos;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FormatterUtils.formatBytes;
import static net.microfalx.maven.extension.MavenUtils.formatDuration;
import static net.microfalx.maven.extension.MavenUtils.formatInteger;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

/**
 * Collects summaries about Maven execution.
 */
@Named
@Singleton
public class ProfilerMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerMetrics.class);

    private static final int LINE_LENGTH = 72;

    private final Map<Class<?>, MojoMetrics> mojoMetrics = new ConcurrentHashMap<>();
    private final long startTime = System.nanoTime();
    private long sessionStartTime;
    private long sessionEndTime;

    @Inject
    protected MavenSession session;

    @Inject
    protected SurefireTests tests;

    void sessionStart() {
        sessionStartTime = System.nanoTime();
    }

    void sessionsEnd() {
        sessionEndTime = System.nanoTime();
    }

    void mojoStarted(Mojo mojo, MojoExecution execution) {
        requireNonNull(mojo);
        getMetrics(mojo).start(execution);
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
        LOGGER.info("");
        infoLine('-');
        LOGGER.info(buffer().strong("Build Report for "
                + session.getTopLevelProject().getName() + " " + session.getTopLevelProject().getVersion()
                + " (" + getDurationReport() + ")").toString());
        LOGGER.info("");
        printTaskSummary();
        LOGGER.info("");
        printTests();
        LOGGER.info("");
        printInfrastructure();
        infoLine('-');
    }

    private void printInfrastructure() {
        infoMain("Infrastructure:");
        LOGGER.info("");
        VirtualMachine virtualMachine = VirtualMachine.get(true);
        Os os = virtualMachine.getOs();
        LOGGER.info(printNameValue("Operating system", os.getName() + " " + os.getVersion()));
        Server server = virtualMachine.getServer();
        LOGGER.info(printNameValue("Hostname", server.getHostName()));
        LOGGER.info(printNameValue("CPU Cores", Integer.toString(server.getCores())));
        LOGGER.info(printNameValue("Memory", formatBytes(server.getMemoryActuallyUsed()) + " of "
                + formatBytes(server.getMemoryTotal())));
        LOGGER.info(printNameValue("User Name", JvmUtils.getUserName()));
        LOGGER.info(printNameValue("Java VM", virtualMachine.getName()));
        LOGGER.info(printNameValue("Java Heap", formatBytes(virtualMachine.getHeapUsedMemory()) + " of "
                + formatBytes(virtualMachine.getHeapTotalMemory())));
    }

    private void printTests() {
        tests.load(session);
        String totals = getTestsReport(tests.getTotalCount(), tests.getFailedCount(),
                tests.getErrorCount(), tests.getSkippedCount());
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
        if (!LOGGER.isInfoEnabled()) return;
        infoMain("Tasks Summary:");
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

    private String printNameValue(String name, String value) {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(name);
        buffer.append(' ');
        MavenUtils.appendDots(buffer, MavenUtils.SHORT_NAME_LENGTH);
        buffer.append(buffer().strong(value));
        return buffer.toString();
    }

    private String getDurationReport() {
        return "Configuration: " + formatDuration(getConfigurationDuration().toMillis())
                + ", Execution: " + formatDuration((getSessionDuration().toMillis()));
    }

    private boolean hasFailures(MavenProject project, Collection<ReportTestSuite> testSuites) {
        int totalFailures = getSum(testSuites, ReportTestSuite::getNumberOfFailures) +
                getSum(testSuites, ReportTestSuite::getNumberOfErrors);
        return totalFailures > 0;
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

    private int getSum(Collection<ReportTestSuite> testSuites, Function<ReportTestSuite, Integer> function) {
        int total = 0;
        for (ReportTestSuite testSuite : testSuites) {
            total += function.apply(testSuite);
        }
        return total;
    }

    private static class MojoMetrics implements Nameable {

        private final String name;
        private final Class<?> clazz;
        private long startTime = System.nanoTime();
        private volatile long endTime;
        private volatile Duration duration;
        private Set<String> goals = new HashSet<>();
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicLong durationNano = new AtomicLong(0);
        private volatile Throwable throwable;

        public MojoMetrics(Mojo mojo) {
            this.clazz = mojo.getClass();
            this.name = MavenUtils.getName(mojo);
        }

        @Override
        public String getName() {
            return name;
        }

        public String getGoal() {
            return String.join(", ", goals);
        }

        public String getClassName() {
            return ClassUtils.getName(clazz);
        }

        void start(MojoExecution execution) {
            startTime = System.nanoTime();
            goals.add(MavenUtils.getGoal(execution));
        }

        void stop(Throwable throwable) {
            endTime = System.nanoTime();
            this.throwable = throwable;
            if (throwable != null) failureCount.incrementAndGet();
            durationNano.addAndGet(endTime - startTime);
            executionCount.incrementAndGet();
        }

        int getExecutionCount() {
            return executionCount.get();
        }

        int getFailureCount() {
            return failureCount.get();
        }

        Duration getDuration() {
            if (this.duration == null) this.duration = ofNanos(durationNano.get());
            return this.duration;
        }
    }
}
