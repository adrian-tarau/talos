package net.microfalx.talos.report;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.lang.*;
import net.microfalx.resource.Resource;
import net.microfalx.talos.core.MavenUtils;
import net.microfalx.talos.model.*;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class ReportHelper {

    private final SessionMetrics session;
    private final ZonedDateTime startupTime = ZonedDateTime.now();

    private List<TestDetails> testDetails;

    public ReportHelper(SessionMetrics session) {
        requireNonNull(session);
        this.session = session;
    }

    public ZonedDateTime getStartupTime() {
        return startupTime;
    }

    public String formatDateTime(Object temporal) {
        return FormatterUtils.formatDateTime(temporal);
    }

    public String formatBytes(Number value) {
        return FormatterUtils.formatBytes(value);
    }

    public String formatPercent(Number value) {
        return FormatterUtils.formatPercent(value);
    }

    public String formatDuration(Duration duration) {
        return MavenUtils.formatDuration(duration, false, false);
    }

    public String formatNumber(Number number) {
        return FormatterUtils.formatNumber(number);
    }

    public String toString(Object value) {
        if (value instanceof Collection<?>) {
            StringBuilder builder = new StringBuilder();
            for (Object o : (Collection<?>) value) {
                StringUtils.append(builder, o, ", ");
            }
            return builder.toString();
        } else {
            return ObjectUtils.toString(value);
        }
    }

    public String toDisplay(Object value) {
        String text = toString(value);
        return StringUtils.isEmpty(text) ? "-" : text;
    }

    public String toHtmlId(Object value) {
        if (value == null) return null;
        return "#" + ObjectUtils.toString(value);
    }

    public long getFailureCount() {
        return session.getProjectFailures().size() + session.getExtensionFailures().size();
    }

    public Duration getTasksDuration() {
        return session.getMojos().stream().map(MojoMetrics::getDuration).reduce(Duration.ZERO, Duration::plus);
    }

    public long getProjectCount() {
        return session.getModules().size();
    }

    public boolean hasTests() {
        return !session.getTests().isEmpty();
    }

    public boolean hasFailures() {
        return hasProjectFailures() || hasExtensionFailures();
    }

    public boolean hasProjectFailures() {
        return !session.getProjectFailures().isEmpty();
    }

    public boolean hasExtensionFailures() {
        return !session.getProjectFailures().isEmpty();
    }

    public double getAverageServerCpu() {
        return session.getServerMetrics().getAverage(ServerMetrics.CPU_TOTAL, Duration.ofDays(1)).orElse(0);
    }

    public double getAverageServerMemory() {
        return session.getServerMetrics().getAverage(ServerMetrics.MEMORY_USED, Duration.ofDays(1)).orElse(0);
    }

    public double getAverageServerLoad1() {
        return session.getServerMetrics().getAverage(ServerMetrics.LOAD_1, Duration.ofDays(1)).orElse(0);
    }

    public double getAverageProcessCpu() {
        return session.getVirtualMachineMetrics().getAverage(VirtualMachineMetrics.CPU_TOTAL, Duration.ofDays(1)).orElse(0);
    }

    public Collection<TestMetrics> getTests(boolean onlyFailing) {
        return session.getTests().stream()
                .filter(testMetrics -> filter(testMetrics, onlyFailing))
                .sorted(Comparator.comparing(TestMetrics::getModuleId).thenComparing(TestMetrics::getClassName)
                        .thenComparing(TestMetrics::getName))
                .collect(Collectors.toList());
    }

    public Collection<MojoMetrics> getMojos() {
        List<MojoMetrics> mojos = new ArrayList<>(session.getMojos());
        mojos.sort(Comparator.comparing(MojoMetrics::getActiveDuration).reversed());
        return mojos;
    }

    public Collection<PluginMetrics> getPlugins() {
        List<PluginMetrics> plugins = new ArrayList<>(session.getPlugins());
        plugins.sort(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId));
        return plugins;
    }

    public Collection<DependencyMetrics> getDependencies(boolean transitive) {
        return session.getDependencies().stream()
                .filter(dependencyMetrics -> filter(dependencyMetrics, transitive))
                .sorted(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId))
                .collect(Collectors.toList());
    }

    public Collection<DependencyDetails> getDependencyDetails(boolean transitive, boolean sortByCount) {
        Map<String, ReportHelper.DependencyDetails> dependencies = new HashMap<>();
        for (DependencyMetrics dependencyMetrics : getDependencies(transitive)) {
            DependencyDetails dependencyDetails = dependencies.computeIfAbsent(dependencyMetrics.getGroupId(), DependencyDetails::new);
            dependencyDetails.count++;
            dependencyDetails.size += Math.max(0, dependencyMetrics.getSize());
        }
        List<DependencyDetails> dependencyDetails = new ArrayList<>(dependencies.values());
        if (sortByCount) {
            dependencyDetails.sort(Comparator.comparing(DependencyDetails::getCount).reversed());
        } else {
            dependencyDetails.sort(Comparator.comparing(DependencyDetails::getSize).reversed());
        }
        return dependencyDetails;
    }

    public Collection<ArtifactMetrics> getArtifacts() {
        List<ArtifactMetrics> artifacts = new ArrayList<>(session.getArtifacts());
        artifacts.sort(Comparator.comparing(Dependency::getGroupId).thenComparing(Dependency::getArtifactId));
        return artifacts;
    }

    public Collection<ProjectMetrics> getModules() {
        List<ProjectMetrics> artifacts = new ArrayList<>(session.getModules());
        artifacts.sort(Comparator.comparing(NamedIdentityAware::getName));
        return artifacts;
    }

    public Collection<LifecycleMetrics> getLifeCycles() {
        List<LifecycleMetrics> lifeCycle = new ArrayList<>(session.getLifecycles());
        lifeCycle.sort(Comparator.comparing(LifecycleMetrics::getActiveDuration).reversed());
        return lifeCycle;
    }

    public Collection<LifecycleMetrics> getExtensionEvents() {
        List<LifecycleMetrics> extensionEvents = new ArrayList<>(session.getExtensionEvents());
        extensionEvents.sort(Comparator.comparing(LifecycleMetrics::getActiveDuration).reversed());
        return extensionEvents;
    }

    public Collection<TrendMetrics> getTrends() {
        List<TrendMetrics> trends = new ArrayList<>(session.getTrends());
        trends.sort(Comparator.comparing(AbstractSessionMetrics::getStartTime));
        return trends;
    }

    public Collection<Property> getSystemProperties() {
        return session.getSystemProperties().entrySet().stream()
                .map(e -> new Property(e.getKey(), e.getValue())).sorted(Comparator.comparing(Property::getName))
                .collect(Collectors.toList());
    }

    public Collection<Property> getProperties() {
        return session.getProject().getProperties().entrySet().stream()
                .map(e -> new Property(e.getKey(), e.getValue())).sorted(Comparator.comparing(Property::getName))
                .collect(Collectors.toList());
    }

    public Collection<ProjectDetails> getProjectDetails() {
        Map<Project, ProjectDetails> projectDetails = new HashMap<>();
        for (PluginMetrics pluginMetrics : session.getPlugins()) {
            for (Project project : pluginMetrics.getProjects()) {
                ProjectDetails details = projectDetails.computeIfAbsent(project, ProjectDetails::new);
                details.plugins.add(pluginMetrics);
            }
        }
        List<ProjectDetails> details = new ArrayList<>(projectDetails.values());
        details.sort(Comparator.comparing(p -> p.getProject().getName()));
        return details;
    }

    public TestSummary getTestSummary() {
        TestSummary summary = new TestSummary();
        for (TestDetails testDetail : getTestDetails()) {
            summary.total += testDetail.total;
            summary.failed += testDetail.failed;
            summary.error += testDetail.error;
            summary.skipped += testDetail.skipped;
            summary.duration = summary.duration.plus(testDetail.duration);
        }
        return summary;
    }

    public List<TestDetails> getTestDetails() {
        if (testDetails != null) return testDetails;
        Map<String, TestDetails> testDetails = new HashMap<>();
        for (TestMetrics testMetrics : session.getTests()) {
            TestDetails tests = testDetails.computeIfAbsent(testMetrics.getModuleId(), s -> new TestDetails(s, session.getModule(s).getName()));
            tests.total++;
            tests.duration = tests.duration.plus(Duration.ofMillis((long) (testMetrics.getTime() * 1000L)));
            if (testMetrics.isFailure()) tests.failed++;
            if (testMetrics.isError()) tests.error++;
            if (testMetrics.isSkipped()) tests.skipped++;
        }
        this.testDetails = new ArrayList<>(testDetails.values());
        this.testDetails.sort(Comparator.comparing(TestDetails::getModule));
        return this.testDetails;
    }

    public List<TestFailureType> getTestFailureTypes() {
        Map<String, TestFailureType> testDetails = new HashMap<>();
        List<TestMetrics> tests = session.getTests().stream().filter(TestMetrics::isFailureOrError)
                .filter(t -> StringUtils.isNotEmpty(t.getFailureType())).collect(Collectors.toUnmodifiableList());
        for (TestMetrics testMetrics : tests) {
            TestFailureType failureType = testDetails.computeIfAbsent(testMetrics.getFailureType(), TestFailureType::new);
            failureType.total++;
        }
        List<TestFailureType> testFailureTypes = new ArrayList<>(testDetails.values());
        testFailureTypes.sort(Comparator.comparing(TestFailureType::getName));
        return testFailureTypes;
    }

    public List<Integer> getTestDurationDistribution() {
        int[] buckets = new int[DURATION_BUCKETS_LENGTH];
        for (TestMetrics testMetrics : session.getTests()) {
            long duration = (long) (testMetrics.getTime() * 1000);
            if (duration > DURATION_BUCKETS[DURATION_BUCKETS_LENGTH - 1]) {
                buckets[DURATION_BUCKETS_LENGTH - 1]++;
            } else if (duration < DURATION_BUCKETS[0]) {
                buckets[0]++;
            } else {
                for (int index = DURATION_BUCKETS_LENGTH - 2; index >= 0; index--) {
                    if (duration >= DURATION_BUCKETS[index]) {
                        buckets[index == 0 ? index + 1 : index]++;
                        break;
                    }
                }
            }
        }
        return Arrays.stream(buckets).boxed().collect(Collectors.toList());
    }

    public String getLogAsHtml() {
        AnsiToHtml ansiToHtml = new AnsiToHtml();
        try {
            Resource resource = ansiToHtml.transform(Resource.text(session.getLogs()));
            return resource.loadAsString();
        } catch (IOException e) {
            return "#ERROR: " + ExceptionUtils.getRootCauseMessage(e);
        }
    }

    private boolean filter(DependencyMetrics dependencyMetrics, boolean transitive) {
        return transitive || !dependencyMetrics.isTransitive();
    }

    private boolean filter(TestMetrics testMetrics, boolean onlyFailing) {
        return !onlyFailing || testMetrics.isFailureOrError();
    }

    private static String getDomain(String groupId) {
        String[] parts = StringUtils.split(groupId, ".");
        if (parts.length < 2) return groupId;
        return parts[0] + "." + parts[1];
    }

    public static class ProjectDetails {

        private final Project project;
        private final Collection<PluginMetrics> plugins = new ArrayList<>();

        public ProjectDetails(Project project) {
            requireNonNull(project);
            this.project = project;
        }

        public Project getProject() {
            return project;
        }

        public Collection<PluginMetrics> getPlugins() {
            return plugins;
        }
    }

    public static class DependencyDetails {

        private final String groupId;
        private int count;
        private long size;

        public DependencyDetails(String groupId) {
            this.groupId = groupId;
        }

        public String getGroupId() {
            return groupId;
        }

        public int getCount() {
            return count;
        }

        public long getSize() {
            return size;
        }
    }

    public static class TestSummary {

        private int total;
        private int failed;
        private int error;
        private int skipped;
        private Duration duration = Duration.ZERO;

        public int getTotal() {
            return total;
        }

        public int getFailed() {
            return failed;
        }

        public int getError() {
            return error;
        }

        public int getFailedAndError() {
            return failed + error;
        }

        public int getSkipped() {
            return skipped;
        }

        public Duration getDuration() {
            return duration;
        }
    }

    public static class TestDetails implements Nameable {

        private final String module;
        private final String name;
        private int total;
        private int failed;
        private int error;
        private int skipped;
        private Duration duration = Duration.ZERO;

        public TestDetails(String module, String name) {
            requireNonNull(module);
            this.module = module;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getModule() {
            return module;
        }

        public int getTotal() {
            return total;
        }

        public int getFailed() {
            return failed;
        }

        public int getError() {
            return error;
        }

        public int getSkipped() {
            return skipped;
        }

        public Duration getDuration() {
            return duration;
        }
    }

    public static class TestFailureType implements Nameable {

        private final String name;
        private int total;

        public TestFailureType(String name) {
            requireNonNull(name);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        public int getTotal() {
            return total;
        }
    }

    public static class Property {

        private final String name;
        private final String value;

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    static final long[] DURATION_BUCKETS = new long[]{
            1, 5, 10, 20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 20_000, 30_000, 60_000
    };
    private static final int DURATION_BUCKETS_LENGTH = DURATION_BUCKETS.length;

    static final String[] DURATION_BUCKET_NAMES = new String[]{
            "<1ms", "5ms", "10ms", "20ms", "50ms", "100ms", "200ms", "500ms", "1s", "2s", "5s", "10s", "20s", "30s", ">60s"
    };

}
