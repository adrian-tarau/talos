package net.microfalx.talos.report;

import net.microfalx.lang.Identifiable;
import net.microfalx.metrics.SeriesStore;
import net.microfalx.talos.model.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class TrendHelper {

    private final SessionMetrics session;
    private final ReportHelper reportHelper;

    private SeriesStore virtualMachineMetrics;
    private SeriesStore serverMetrics;

    public TrendHelper(SessionMetrics session, ReportHelper reportHelper) {
        requireNonNull(session);
        requireNonNull(reportHelper);
        this.session = session;
        this.reportHelper = reportHelper;
    }

    public boolean hasTrends() {
        return session.getTrends().size() > 2;
    }

    public SeriesStore getVirtualMachineMetrics() {
        if (virtualMachineMetrics == null) {
            virtualMachineMetrics = SeriesStore.memory();
            session.getTrends().stream().map(TrendMetrics::getVirtualMachineMetrics)
                    .forEach(store -> virtualMachineMetrics.add(store));
        }
        return virtualMachineMetrics;
    }

    public SeriesStore getServerMetrics() {
        if (serverMetrics == null) {
            serverMetrics = SeriesStore.memory();
            session.getTrends().stream().map(TrendMetrics::getServerMetrics)
                    .forEach(store -> serverMetrics.add(store));
        }
        return serverMetrics;
    }

    public Collection<LifecycleMetrics> getLifecycleMetricsTypes() {
        return getTypes(AbstractSessionMetrics::getLifecycles);
    }

    public Collection<LifecycleMetrics> getLifecycleMetrics(String id) {
        return getMetrics(id, (trend) -> trend.getLifecycle(id));
    }

    public Collection<MojoMetrics> getMojoMetricsTypes() {
        return getTypes(AbstractSessionMetrics::getMojos);
    }

    public Collection<MojoMetrics> getMojoMetrics(String id) {
        return getMetrics(id, (trend) -> trend.getMojo(id));
    }

    public Collection<TestSummaryMetrics> getTestCountsMetrics() {
        return TestSummaryMetrics.summaries(reportHelper.getTrends());
    }

    public Map<ProjectMetrics, Collection<ModuleFailures>> getTestFailuresByModule() {
        Map<ProjectMetrics, Collection<ModuleFailures>> failures = new HashMap<>();
        Set<ProjectMetrics> toKeep = new HashSet<>();
        for (TrendMetrics trendMetrics : reportHelper.getTrends()) {
            for (TestSummaryMetrics metrics : trendMetrics.getTests()) {
                ProjectMetrics module = session.getModule(metrics.getModuleId());
                Collection<ModuleFailures> failuresByModule = failures.computeIfAbsent(module, id -> new ArrayList<>());
                ModuleFailures moduleFailures = new ModuleFailures(module, trendMetrics.getStartTime());
                moduleFailures.count += metrics.getFailure() + metrics.getError();
                failuresByModule.add(moduleFailures);
            }
        }
        for (Map.Entry<ProjectMetrics, Collection<ModuleFailures>> entry : failures.entrySet()) {
            for (ModuleFailures moduleFailures : entry.getValue()) {
                if (moduleFailures.count > 0) {
                    toKeep.add(entry.getKey());
                    break;
                }
            }
        }
        Map<ProjectMetrics, Collection<ModuleFailures>> finalFailures = new HashMap<>();
        for (ProjectMetrics module : toKeep) {
            finalFailures.put(module, failures.getOrDefault(module, Collections.emptyList()));
        }
        return finalFailures;
    }

    private <T extends Identifiable<String>> Collection<T> getTypes(Function<TrendMetrics, Collection<T>> mapper) {
        Map<String, T> types = new HashMap<>();
        for (TrendMetrics trend : session.getTrends()) {
            for (T type : mapper.apply(trend)) {
                types.computeIfAbsent(type.getId(), s -> type);
            }
        }
        return types.values();
    }

    public <T extends TimeAwareMetrics<T>> Collection<T> getMetrics(String id, Function<TrendMetrics, T> mapper) {
        Collection<T> metrics = new ArrayList<>();
        for (TrendMetrics trend : reportHelper.getTrends()) {
            try {
                T value = mapper.apply(trend);
                if (value != null) metrics.add(value.updateInterval(trend.getStartTime(), trend.getEndTime()));
            } catch (IllegalArgumentException e) {
                // ignore "cannot find this id"
            }
        }
        return metrics;
    }

    public static class ModuleFailures {

        private final ProjectMetrics module;
        private final ZonedDateTime startTime;
        private int count;

        public ModuleFailures(ProjectMetrics module, ZonedDateTime startTime) {
            this.module = module;
            this.startTime = startTime;
        }

        public ProjectMetrics getModule() {
            return module;
        }

        public ZonedDateTime getStartTime() {
            return startTime;
        }

        public int getCount() {
            return count;
        }
    }
}
