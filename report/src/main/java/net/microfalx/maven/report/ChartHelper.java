package net.microfalx.maven.report;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.lang.Nameable;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.lang.TimeUtils;
import net.microfalx.maven.model.*;
import net.microfalx.metrics.SeriesStore;
import net.microfalx.metrics.Value;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FormatterUtils.formatNumber;
import static net.microfalx.lang.StringUtils.EMPTY_STRING;
import static net.microfalx.lang.TimeUtils.toMillis;

public class ChartHelper {

    private static final long offsetMillis = ZonedDateTime.now().getOffset().getTotalSeconds() * TimeUtils.MILLISECONDS_IN_SECOND;

    private final SessionMetrics session;
    private final ReportHelper reportHelper;
    private final TrendHelper trendHelper;
    private final CodeCoverageHelper codeCoverageHelper;

    public ChartHelper(SessionMetrics session, ReportHelper reportHelper, TrendHelper trendHelper, CodeCoverageHelper codeCoverageHelper) {
        requireNonNull(session);
        requireNonNull(reportHelper);
        requireNonNull(trendHelper);
        requireNonNull(codeCoverageHelper);
        this.session = session;
        this.reportHelper = reportHelper;
        this.trendHelper = trendHelper;
        this.codeCoverageHelper = codeCoverageHelper;
    }

    public PieChart<Integer> getTotalTestsPieChart(String id) {
        PieChart<Integer> chart = new PieChart<>(id, "Total");
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getTotal()));
        return chart;
    }

    public PieChart<Integer> getFailedTestsPieChart(String id) {
        PieChart<Integer> chart = new PieChart<>(id, "Failed & Error");
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getFailed() + report.getError()));
        return chart;
    }

    public PieChart<Long> getDurationTestsPieChart(String id) {
        PieChart<Long> chart = new PieChart<>(id, "Durations");
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getDuration().toMillis()));
        return chart;
    }

    public PieChart<Long> getBuildEventsPieChart(String id) {
        PieChart<Long> chart = new PieChart<>(id, "Build Events");
        chart.getLegend().setShow(false);
        reportHelper.getLifeCycles().forEach(event -> chart.add(event.getName(), event.getActiveDuration().toMillis()));
        return chart;
    }

    public PieChart<Long> getExtensionEventsPieChart(String id) {
        PieChart<Long> chart = new PieChart<>(id, "Extension Events");
        chart.getLegend().setShow(false);
        reportHelper.getExtensionEvents().forEach(event -> chart.add(event.getName(), event.getActiveDuration().toMillis()));
        return chart;
    }

    public TreeMapChart<Integer> getTotalTestsTreeMapChart(String id) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Total");
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getTotal()));
        return chart;
    }

    public TreeMapChart<Integer> getFailedTestsTreeMapChart(String id) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Failed");
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getFailed()));
        return chart;
    }

    public TreeMapChart<Integer> getErrorTestsTreeMapChart(String id) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Error");
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getError()));
        return chart;
    }

    public TreeMapChart<Integer> getSkippedTestsTreeMapChart(String id) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Skipped");
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getSkipped()));
        return chart;
    }

    public TreeMapChart<Long> getDurationTestsTreeMapChart(String id) {
        TreeMapChart<Long> chart = new TreeMapChart<>(id, "Durations");
        chart.getLegend().setShow(false);
        chart.getYaxis().setUnit(Unit.DURATION);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getDuration().toMillis()));
        return chart;
    }

    public BarChart<Integer> getTestFailureTypesBarChart(String id) {
        BarChart<Integer> chart = new BarChart<>(id, "Failure Types");
        chart.setSeriesName("Tests");
        chart.getLegend().setShow(false);
        reportHelper.getTestFailureTypes().forEach(report -> chart.add(report.getName(), report.getTotal()));
        return chart;
    }

    public ColumnChart<Integer> getTestDurationDistributionColumnChart(String id) {
        ColumnChart<Integer> chart = new ColumnChart<>(id, "Duration Distribution");
        chart.setSeriesName("Tests");
        chart.getLegend().setShow(false);
        List<Integer> values = reportHelper.getTestDurationDistribution();
        for (int index = 0; index < values.size(); index++) {
            chart.add(ReportHelper.DURATION_BUCKET_NAMES[index], values.get(index));
        }
        return chart;
    }

    public AreaChart<Long, Float> getSessionServerCpu(String id) {
        return getServerCpu(id, session.getServerMetrics());
    }

    public AreaChart<Long, Float> getTrendServerCpu(String id) {
        return getServerCpu(id, trendHelper.getServerMetrics());
    }

    public AreaChart<Long, Float> getServerCpu(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "CPU");
        chart.add(convert("System", store.get(ServerMetrics.CPU_SYSTEM)));
        chart.add(convert("User", store.get(ServerMetrics.CPU_USER)));
        chart.add(convert("Nice", store.get(ServerMetrics.CPU_NICE)));
        chart.add(convert("I/O Wait", store.get(ServerMetrics.CPU_IO_WAIT)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.PERCENT);
        return chart;
    }

    public AreaChart<Long, Float> getSessionServerLoad(String id) {
        return getServerLoad(id, session.getServerMetrics());
    }

    public AreaChart<Long, Float> getTrendServerLoad(String id) {
        return getServerLoad(id, trendHelper.getServerMetrics());
    }

    public AreaChart<Long, Float> getServerLoad(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Load");
        chart.add(convert("Load", store.get(ServerMetrics.LOAD_1)));
        chart.setStacked(true);
        return chart;
    }

    public AreaChart<Long, Float> getSessionServerKernel(String id) {
        return getServerKernel(id, session.getServerMetrics());
    }

    public AreaChart<Long, Float> getTrendServerKernel(String id) {
        return getServerKernel(id, trendHelper.getServerMetrics());
    }

    public AreaChart<Long, Float> getServerKernel(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Kernel");
        chart.add(convert("Context Switches", store.get(ServerMetrics.CONTEXT_SWITCHES)));
        chart.add(convert("Interrupts", store.get(ServerMetrics.INTERRUPTS)));
        return chart;
    }

    public AreaChart<Long, Float> getSessionServerIOCounts(String id) {
        return getServerIOCounts(id, session.getServerMetrics());
    }

    public AreaChart<Long, Float> getTrendServerIOCounts(String id) {
        return getServerIOCounts(id, trendHelper.getServerMetrics());
    }

    public AreaChart<Long, Float> getServerIOCounts(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "IO / Activity");
        chart.add(convert("Reads", store.get(ServerMetrics.IO_READS)));
        chart.add(convert("Writes", store.get(ServerMetrics.IO_WRITES)));
        return chart;
    }

    public AreaChart<Long, Float> getSessionServerIOBytes(String id) {
        return getServerIOBytes(id, session.getServerMetrics());
    }

    public AreaChart<Long, Float> getTrendServerIOBytes(String id) {
        return getServerIOBytes(id, trendHelper.getServerMetrics());
    }

    public AreaChart<Long, Float> getServerIOBytes(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "IO / Bytes");
        chart.add(convert("Read Bytes", store.get(ServerMetrics.IO_READ_BYTES)));
        chart.add(convert("Write Bytes", store.get(ServerMetrics.IO_WRITE_BYTES)));
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public AreaChart<Long, Float> getSessionServerMemory(String id) {
        return getServerMemory(id, session.getServerMetrics());
    }

    public AreaChart<Long, Float> getTrendServerMemory(String id) {
        return getServerMemory(id, trendHelper.getServerMetrics());
    }

    public AreaChart<Long, Float> getServerMemory(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Memory");
        chart.add(convert("Maximum", store.get(ServerMetrics.MEMORY_MAX)));
        chart.add(convert("Used", store.get(ServerMetrics.MEMORY_USED)));
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public AreaChart<Long, Float> getSessionProcessCpu(String id) {
        return getProcessCpu(id, session.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getTrendProcessCpu(String id) {
        return getProcessCpu(id, trendHelper.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getProcessCpu(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "CPU");
        chart.add(convert("System", store.get(VirtualMachineMetrics.CPU_SYSTEM)));
        chart.add(convert("User", store.get(VirtualMachineMetrics.CPU_USER)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.PERCENT);
        return chart;
    }

    public AreaChart<Long, Float> getSessionProcessMemory(String id) {
        return getProcessMemory(id, session.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getTrendProcessMemory(String id) {
        return getProcessMemory(id, trendHelper.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getProcessMemory(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Memory");
        chart.add(convert("Heap", store.get(VirtualMachineMetrics.MEMORY_HEAP_USED)));
        chart.add(convert("Non-Heap", store.get(VirtualMachineMetrics.MEMORY_NON_HEAP_USED)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public AreaChart<Long, Float> getSessionProcessThreads(String id) {
        return getProcessThreads(id, session.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getTrendProcessThreads(String id) {
        return getProcessThreads(id, trendHelper.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getProcessThreads(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Threads");
        chart.add(convert("Daemon", store.get(VirtualMachineMetrics.THREAD_DAEMON)));
        chart.add(convert("Non-Daemon", store.get(VirtualMachineMetrics.THREAD_NON_DAEMON)));
        chart.setStacked(true);
        return chart;
    }

    public AreaChart<Long, Float> getSessionProcessIO(String id) {
        return getProcessIO(id, session.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getTrendProcessIO(String id) {
        return getProcessIO(id, trendHelper.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getProcessIO(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "IO");
        chart.add(convert("Read Bytes", store.get(VirtualMachineMetrics.IO_READ_BYTES)));
        chart.add(convert("Write Bytes", store.get(VirtualMachineMetrics.IO_WRITE_BYTES)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public AreaChart<Long, Float> getSessionProcessGcCounts(String id) {
        return getProcessGcCounts(id, session.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getTrendProcessGcCounts(String id) {
        return getProcessGcCounts(id, trendHelper.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getProcessGcCounts(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "GC / Collections");
        chart.add(convert("Eden", store.get(VirtualMachineMetrics.GC_EDEN_COUNT)));
        chart.add(convert("Tenured", store.get(VirtualMachineMetrics.GC_TENURED_COUNT)));
        chart.setStacked(true);
        return chart;
    }

    public AreaChart<Long, Float> getSessionProcessGcDuration(String id) {
        return getProcessGcDuration(id, session.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getTrendProcessGcDuration(String id) {
        return getProcessGcDuration(id, trendHelper.getVirtualMachineMetrics());
    }

    public AreaChart<Long, Float> getProcessGcDuration(String id, SeriesStore store) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "GC / Durations");
        chart.add(convert("Eden", store.get(VirtualMachineMetrics.GC_EDEN_DURATION)));
        chart.add(convert("Tenured", store.get(VirtualMachineMetrics.GC_TENURED_DURATION)));
        chart.getYaxis().setUnit(Unit.DURATION);
        return chart;
    }

    public TreeMapChart<Integer> getDependenciesCountTreeMapChart(String id) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Dependency Count By Group");
        chart.setHeight(500);
        chart.getLegend().setShow(false);
        reportHelper.getDependencyDetails(true, true).forEach(d -> chart.add(d.getGroupId(), d.getCount()));
        return chart;
    }

    public TreeMapChart<Long> getDependenciesSizeTreeMapChart(String id) {
        TreeMapChart<Long> chart = new TreeMapChart<>(id, "Dependency Size By Group");
        chart.setHeight(500);
        chart.getLegend().setShow(false);
        chart.getYaxis().setUnit(Unit.BYTE);
        reportHelper.getDependencyDetails(true, false).forEach(d -> chart.add(d.getGroupId(), d.getSize()));
        return chart;
    }

    public AreaChart<Long, Float> getTrendSessionDuration(String id) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Sessions");
        chart.add(convert("Duration", reportHelper.getTrends(), m -> toMillis(m.getStartTime()),
                m -> (float) m.getDuration().toMillis()));
        chart.setHeight(300);
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.DURATION);
        return chart;
    }

    public AreaChart<Long, Float> getTrendEventsDuration(String id) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Events");
        for (LifecycleMetrics metrics : trendHelper.getLifecycleMetricsTypes()) {
            chart.add(convert(metrics.getName(), trendHelper.getLifecycleMetrics(metrics.getId()), m -> toMillis(m.getStartTime()),
                    m -> (float) m.getActiveDuration().toMillis()));
        }
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.DURATION);
        return chart;
    }

    public AreaChart<Long, Float> getTrendTasksDuration(String id) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Tasks");
        for (MojoMetrics metrics : trendHelper.getMojoMetricsTypes()) {
            chart.add(convert(metrics.getName(), trendHelper.getMojoMetrics(metrics.getId()), m -> toMillis(m.getStartTime()),
                    m -> (float) m.getActiveDuration().toMillis()));
        }
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.DURATION);
        return chart;
    }

    public AreaChart<Long, Float> getTrendTestCounts(String id) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Tests Summary");
        Collection<TestSummaryMetrics> testCountsMetrics = trendHelper.getTestCountsMetrics();
        chart.add(convert("Passed", testCountsMetrics, m -> toMillis(m.getStartTime()),
                m -> (float) m.getPassed()));
        chart.add(convert("Failed", testCountsMetrics, m -> toMillis(m.getStartTime()),
                m -> (float) m.getFailure()));
        chart.add(convert("Errors", testCountsMetrics, m -> toMillis(m.getStartTime()),
                m -> (float) m.getError()));
        chart.add(convert("Skipped", testCountsMetrics, m -> toMillis(m.getStartTime()),
                m -> (float) m.getSkipped()));
        chart.setStacked(true);
        return chart;
    }

    public AreaChart<Long, Float> getTrendTestFailuresByModuleCounts(String id) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Tests Failures");
        Map<ProjectMetrics, Collection<TrendHelper.ModuleFailures>> testFailuresByModule = trendHelper.getTestFailuresByModule();
        for (Map.Entry<ProjectMetrics, Collection<TrendHelper.ModuleFailures>> entry : testFailuresByModule.entrySet()) {
            ProjectMetrics module = entry.getKey();
            Collection<TrendHelper.ModuleFailures> failures = entry.getValue();
            chart.add(convert(module.getName(), failures, m -> toMillis(m.getStartTime()),
                    m -> (float) m.getCount()));
        }
        chart.setStacked(true);
        return chart;
    }

    private static Series<Long, Float> convert(String name, net.microfalx.metrics.Series metricsSeries) {
        Series<Long, Float> series = new Series<>(name);
        for (Value value : metricsSeries.getValues()) {
            series.add(toMillisLocalZone(value.getTimestamp()), round(value.asFloat()));
        }
        return series;
    }

    private static <T> Series<Long, Float> convert(String name, Iterable<T> items, Function<T, Long> timestampFunction,
                                                   Function<T, Float> valueFunction) {
        Series<Long, Float> series = new Series<>(name);
        for (T item : items) {
            series.add(toMillisLocalZone(timestampFunction.apply(item)), round(valueFunction.apply(item)));
        }
        return series;
    }

    private static long toMillisLocalZone(long millis) {
        return millis + offsetMillis;
    }

    private static <V extends Number> String toString(V value) {
        if (value == null) {
            return "0";
        } else {
            return formatNumber(value, 2, EMPTY_STRING);
        }
    }

    private static float round(float value) {
        if (value < 0.001) {
            return 0;
        } else {
            return value;
        }
    }

    public enum Unit {
        COUNT,
        DATE_TIME,
        DURATION,
        BYTE,
        PERCENT
    }

    public static class Data<X, Y extends Number> {

        private X x;
        private Y y;

        public Data(X x, Y y) {
            requireNonNull(x);
            this.x = x;
            this.y = y;
        }

        public X getX() {
            return x;
        }

        public Y getY() {
            return y;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Data.class.getSimpleName() + "[", "]")
                    .add("x=" + x)
                    .add("y=" + y)
                    .toString();
        }
    }

    public static class Series<X, Y extends Number> implements Nameable {

        private final String name;
        private final Collection<Data<X, Y>> data = new ArrayList<>();

        public Series(String name) {
            requireNonNull(name);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        public Collection<Data<X, Y>> getData() {
            return data;
        }

        public Series<X, Y> add(Data<X, Y> data) {
            this.data.add(data);
            return this;
        }

        public Series<X, Y> add(X x, Y y) {
            requireNonNull(x);
            requireNonNull(y);
            add(new Data<>(x, y));
            return this;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Series.class.getSimpleName() + "[", "]")
                    .add("name='" + name + "'")
                    .add("data=" + data)
                    .toString();
        }
    }

    public static class Legend {

        private boolean show = true;

        public boolean isShow() {
            return show;
        }

        public Legend setShow(boolean show) {
            this.show = show;
            return this;
        }
    }

    public static class Axis {

        private Unit unit = Unit.COUNT;

        public Unit getUnit() {
            return unit;
        }

        public Axis setUnit(Unit unit) {
            requireNonNull(unit);
            this.unit = unit;
            return this;
        }
    }

    public static class Chart extends NamedIdentityAware<String> {

        private Integer width;
        private Integer height;
        private Legend legend = new Legend();
        private Axis xaxis = new Axis();
        private Axis yaxis = new Axis();

        public Chart(String id, String name) {
            setId(id);
            setName(name);
        }

        public Integer getWidth() {
            return width;
        }

        public Chart setWidth(Integer width) {
            this.width = width;
            return this;
        }

        public Integer getHeight() {
            return height;
        }

        public Chart setHeight(Integer height) {
            this.height = height;
            return this;
        }

        public Legend getLegend() {
            return legend;
        }

        public Chart setLegend(Legend legend) {
            this.legend = legend;
            return this;
        }

        public Axis getXaxis() {
            return xaxis;
        }

        public Chart setXaxis(Axis xaxis) {
            this.xaxis = xaxis;
            return this;
        }

        public Axis getYaxis() {
            return yaxis;
        }

        public Chart setYaxis(Axis yaxis) {
            this.yaxis = yaxis;
            return this;
        }
    }

    public static abstract class DataChart<X, Y extends Number> extends Chart {

        private final Collection<Data<X, Y>> data = new ArrayList<>();

        public DataChart(String id, String name) {
            super(id, name);
        }

        public Collection<Data<X, Y>> getData() {
            List<Data<X, Y>> sorted = new ArrayList<>(data);
            sorted.sort((o1, o2) -> -Double.compare(o1.getY().doubleValue(), o2.getY().doubleValue()));
            return unmodifiableCollection(sorted);
        }

        public DataChart<X, Y> add(X x, Y y) {
            requireNonNull(x);
            data.add(new Data<>(x, y));
            return this;
        }
    }

    public static abstract class MultiSeriesChart<X, Y extends Number> extends Chart {

        private final List<Series<X, Y>> series = new ArrayList<>();

        public MultiSeriesChart(String id, String name) {
            super(id, name);
        }

        public List<Series<X, Y>> getSeries() {
            return unmodifiableList(series);
        }

        public String getXDataType() {
            for (Series<X, Y> s : series) {
                for (Data<X, Y> data : s.getData()) {
                    X x = data.getX();
                    if (x instanceof Temporal || x instanceof Long) {
                        return "datetime";
                    }
                }
            }
            return "datetime";
        }

        public MultiSeriesChart<X, Y> add(Series<X, Y> series) {
            requireNonNull(series);
            this.series.add(series);
            return this;
        }
    }

    public static abstract class SingleSeriesChart<N extends Number> extends Chart {

        private final List<N> series = new ArrayList<>();
        private final List<String> labels = new ArrayList<>();
        private String seriesName = "Value";

        public SingleSeriesChart(String id, String name) {
            super(id, name);
        }

        public List<N> getSeries() {
            return unmodifiableList(series);
        }

        public List<String> getLabels() {
            return unmodifiableList(labels);
        }

        public String getSeriesName() {
            return seriesName;
        }

        public SingleSeriesChart<N> setSeriesName(String seriesName) {
            requireNonNull(seriesName);
            this.seriesName = seriesName;
            return this;
        }

        public SingleSeriesChart<N> add(String label, N value) {
            requireNonNull(label);
            requireNonNull(value);
            labels.add(label);
            series.add(value);
            return this;
        }

        public SingleSeriesChart<N> addLabels(Iterable<String> labels) {
            requireNonNull(labels);
            labels.forEach(this.labels::add);
            return this;
        }

        public SingleSeriesChart<N> addValues(Iterable<N> values) {
            requireNonNull(labels);
            values.forEach(series::add);
            return this;
        }
    }

    public static class PieChart<N extends Number> extends SingleSeriesChart<N> {

        public PieChart(String id, String name) {
            super(id, name);
        }
    }

    public static class BarChart<N extends Number> extends SingleSeriesChart<N> {

        public BarChart(String id, String name) {
            super(id, name);
        }
    }

    public static class ColumnChart<N extends Number> extends SingleSeriesChart<N> {

        public ColumnChart(String id, String name) {
            super(id, name);
        }
    }

    public static class AreaChart<X, Y extends Number> extends MultiSeriesChart<X, Y> {

        private boolean stacked;

        public AreaChart(String id, String name) {
            super(id, name);
        }

        public boolean isStacked() {
            return stacked;
        }

        public AreaChart<X, Y> setStacked(boolean stacked) {
            this.stacked = stacked;
            return this;
        }
    }

    public static class TreeMapChart<N extends Number> extends DataChart<String, N> {

        public TreeMapChart(String id, String name) {
            super(id, name);
        }
    }
}
