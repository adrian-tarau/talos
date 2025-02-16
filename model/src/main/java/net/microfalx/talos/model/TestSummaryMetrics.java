package net.microfalx.talos.model;

import net.microfalx.lang.NamedIdentityAware;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableSet;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class TestSummaryMetrics extends NamedIdentityAware<String> {

    private String moduleId;
    private int total;
    private int failure;
    private int error;
    private int skipped;
    private final Set<String> failureTypes = new HashSet<>();
    private Duration duration = Duration.ZERO;

    transient ProjectMetrics module;
    transient ZonedDateTime startTime;

    protected TestSummaryMetrics() {
    }

    public TestSummaryMetrics(String moduleId) {
        requireNonNull(moduleId);
        this.moduleId = moduleId;
        setName(moduleId);
        setId(moduleId);
    }

    public String getModuleId() {
        return moduleId;
    }

    public int getTotal() {
        return total;
    }

    public int getPassed() {
        return total - failure - error - skipped;
    }

    public int getFailure() {
        return failure;
    }

    public int getError() {
        return error;
    }

    public int getSkipped() {
        return skipped;
    }

    public ProjectMetrics getModule() {
        return module;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public Set<String> getFailureTypes() {
        return unmodifiableSet(failureTypes);
    }

    public Duration getDuration() {
        return duration;
    }

    public static Collection<TestSummaryMetrics> summaries(Collection<TrendMetrics> metrics) {
        requireNonNull(metrics);
        return metrics.stream().map(t -> {
                    TestSummaryMetrics summary = TestSummaryMetrics.summary(t.getTests());
                    summary.startTime = t.getStartTime();
                    return summary;
                })
                .collect(Collectors.toList());
    }

    public static TestSummaryMetrics summary(Collection<TestSummaryMetrics> metrics) {
        requireNonNull(metrics);
        TestSummaryMetrics summary = new TestSummaryMetrics("all");
        for (TestSummaryMetrics metric : metrics) {
            summary.total += metric.total;
            summary.failure += metric.failure;
            summary.error += metric.error;
            summary.skipped += metric.skipped;
        }
        return summary;
    }

    public static Collection<TestSummaryMetrics> from(Collection<TestMetrics> metrics) {
        requireNonNull(metrics);
        Map<String, TestSummaryMetrics> summaryMetrics = new HashMap<>();
        for (TestMetrics metric : metrics) {
            TestSummaryMetrics summary = summaryMetrics.computeIfAbsent(metric.getModuleId(), TestSummaryMetrics::new);
            summary.add(metric);
        }
        return new ArrayList<>(summaryMetrics.values());
    }

    void add(TestMetrics metrics) {
        total++;
        duration = duration.plus(metrics.getDuration());
        if (metrics.isFailure()) failure++;
        if (metrics.isError()) error++;
        if (metrics.isSkipped()) skipped++;
        failureTypes.add(metrics.getFailureType());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TestSummaryMetrics.class.getSimpleName() + "[", "]")
                .add("moduleId='" + moduleId + "'")
                .add("total=" + total)
                .add("failure=" + failure)
                .add("error=" + error)
                .add("skipped=" + skipped)
                .add("failureTypes=" + failureTypes)
                .add("duration=" + duration)
                .toString();
    }
}
