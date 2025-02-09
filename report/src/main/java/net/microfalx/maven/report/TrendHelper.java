package net.microfalx.maven.report;

import net.microfalx.lang.Identifiable;
import net.microfalx.maven.model.*;
import net.microfalx.metrics.SeriesStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
                if (value != null) metrics.add(value.setStartTime(trend.getStartTime()));
            } catch (IllegalArgumentException e) {
                // ignore "cannot find this id"
            }
        }
        return metrics;
    }
}
