package net.microfalx.maven.report;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.lang.Nameable;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.maven.model.SessionMetrics;
import net.microfalx.metrics.Value;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FormatterUtils.formatNumber;
import static net.microfalx.lang.StringUtils.EMPTY_STRING;

public class ChartHelper {

    private final SessionMetrics session;
    private final ReportHelper reportHelper;

    public ChartHelper(SessionMetrics session, ReportHelper reportHelper) {
        requireNonNull(session);
        requireNonNull(reportHelper);
        this.session = session;
        this.reportHelper = reportHelper;
    }

    public PieChart<Integer> getTotalTestsPieChart(String id, Integer width) {
        PieChart<Integer> chart = new PieChart<>(id, "Total");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getTotal()));
        return chart;
    }

    public PieChart<Integer> getFailedTestsPieChart(String id, Integer width) {
        PieChart<Integer> chart = new PieChart<>(id, "Failed & Error");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getFailed() + report.getError()));
        return chart;
    }

    public PieChart<Long> getDurationTestsPieChart(String id, Integer width) {
        PieChart<Long> chart = new PieChart<>(id, "Durations");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getDuration().toMillis()));
        return chart;
    }

    public PieChart<Long> getBuildEventsPieChart(String id, Integer width) {
        PieChart<Long> chart = new PieChart<>(id, "Build Events");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getLifeCycles().forEach(event -> chart.add(event.getName(), event.getDuration().toMillis()));
        return chart;
    }

    public TreeMapChart<Integer> getTotalTestsTreeMapChart(String id, Integer width) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Total");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getTotal()));
        return chart;
    }

    public TreeMapChart<Integer> getFailedTestsTreeMapChart(String id, Integer width) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Failed");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getFailed()));
        return chart;
    }

    public TreeMapChart<Integer> getErrorTestsTreeMapChart(String id, Integer width) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Error");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getError()));
        return chart;
    }

    public TreeMapChart<Integer> getSkippedTestsTreeMapChart(String id, Integer width) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Skipped");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getSkipped()));
        return chart;
    }

    public TreeMapChart<Long> getDurationTestsTreeMapChart(String id, Integer width) {
        TreeMapChart<Long> chart = new TreeMapChart<>(id, "Durations");
        chart.setWidth(width);
        chart.getLegend().setShow(false);
        chart.getYaxis().setUnit(Unit.DURATION);
        reportHelper.getTestDetails().forEach(report -> chart.add(report.getName(), report.getDuration().toMillis()));
        return chart;
    }

    public BarChart<Integer> getTestFailureTypesBarChart(String id, Integer width) {
        BarChart<Integer> chart = new BarChart<>(id, "Failure Types");
        chart.setSeriesName("Tests").setWidth(width);
        chart.getLegend().setShow(false);
        reportHelper.getTestFailureTypes().forEach(report -> chart.add(report.getName(), report.getTotal()));
        return chart;
    }

    public ColumnChart<Integer> getTestDurationDistributionColumnChart(String id, Integer width) {
        ColumnChart<Integer> chart = new ColumnChart<>(id, "Duration Distribution");
        chart.setSeriesName("Tests").setWidth(width);
        chart.getLegend().setShow(false);
        List<Integer> values = reportHelper.getTestDurationDistribution();
        for (int index = 0; index < values.size(); index++) {
            chart.add(ReportHelper.DURATION_BUCKET_NAMES[index], values.get(index));
        }
        return chart;
    }

    public AreaChart<Long, Float> getServerCpu(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "CPU");
        chart.add(convert("System", session.getServerMetrics().get(ServerMetrics.CPU_SYSTEM)));
        chart.add(convert("User", session.getServerMetrics().get(ServerMetrics.CPU_USER)));
        chart.add(convert("Nice", session.getServerMetrics().get(ServerMetrics.CPU_NICE)));
        chart.add(convert("I/O Wait", session.getServerMetrics().get(ServerMetrics.CPU_IO_WAIT)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.PERCENT);
        return chart;
    }

    public AreaChart<Long, Float> getServerLoad(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Load");
        chart.add(convert("Load", session.getServerMetrics().get(ServerMetrics.LOAD_1)));
        chart.setStacked(true);
        return chart;
    }

    public AreaChart<Long, Float> getServerKernel(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Kernel");
        chart.add(convert("Context Switches", session.getServerMetrics().get(ServerMetrics.CONTEXT_SWITCHES)));
        chart.add(convert("Interrupts", session.getServerMetrics().get(ServerMetrics.INTERRUPTS)));
        return chart;
    }

    public AreaChart<Long, Float> getServerIOCounts(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "IO / Activity");
        chart.add(convert("Reads", session.getServerMetrics().get(ServerMetrics.IO_READS)));
        chart.add(convert("Writes", session.getServerMetrics().get(ServerMetrics.IO_WRITES)));
        return chart;
    }

    public AreaChart<Long, Float> getServerIOBytes(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "IO / Bytes");
        chart.add(convert("Read Bytes", session.getServerMetrics().get(ServerMetrics.IO_READ_BYTES)));
        chart.add(convert("Write Bytes", session.getServerMetrics().get(ServerMetrics.IO_WRITE_BYTES)));
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public AreaChart<Long, Float> getServerMemory(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Memory");
        chart.add(convert("Maximum", session.getServerMetrics().get(ServerMetrics.MEMORY_MAX)));
        chart.add(convert("Used", session.getServerMetrics().get(ServerMetrics.MEMORY_USED)));
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public AreaChart<Long, Float> getProcessCpu(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "CPU");
        chart.add(convert("System", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.CPU_SYSTEM)));
        chart.add(convert("User", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.CPU_USER)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.PERCENT);
        return chart;
    }

    public AreaChart<Long, Float> getProcessMemory(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Memory");
        chart.add(convert("Heap", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.MEMORY_HEAP_USED)));
        chart.add(convert("Non-Heap", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.MEMORY_NON_HEAP_USED)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public AreaChart<Long, Float> getProcessThreads(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "Threads");
        chart.add(convert("Daemon", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.THREAD_DAEMON)));
        chart.add(convert("Non-Daemon", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.THREAD_NON_DAEMON)));
        chart.setStacked(true);
        return chart;
    }

    public AreaChart<Long, Float> getProcessIO(String id, Integer width) {
        AreaChart<Long, Float> chart = new AreaChart<>(id, "IO");
        chart.add(convert("Read Bytes", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.IO_READ_BYTES)));
        chart.add(convert("Write Bytes", session.getVirtualMachineMetrics().get(VirtualMachineMetrics.IO_WRITE_BYTES)));
        chart.setStacked(true);
        chart.getYaxis().setUnit(Unit.BYTE);
        return chart;
    }

    public TreeMapChart<Integer> getDependenciesCountTreeMapChart(String id, Integer width) {
        TreeMapChart<Integer> chart = new TreeMapChart<>(id, "Dependency Count By Group");
        chart.setWidth(width);
        chart.setHeight(500);
        chart.getLegend().setShow(false);
        reportHelper.getDependencyDetails(true, true).forEach(d -> chart.add(d.getGroupId(), d.getCount()));
        return chart;
    }

    public TreeMapChart<Long> getDependenciesSizeTreeMapChart(String id, Integer width) {
        TreeMapChart<Long> chart = new TreeMapChart<>(id, "Dependency Size By Group");
        chart.setWidth(width);
        chart.setHeight(500);
        chart.getLegend().setShow(false);
        chart.getYaxis().setUnit(Unit.BYTE);
        reportHelper.getDependencyDetails(true, false).forEach(d -> chart.add(d.getGroupId(), d.getSize()));
        return chart;
    }

    private static Series<Long, Float> convert(String name, net.microfalx.metrics.Series metricsSeries) {
        Series<Long, Float> series = new Series<>(name);
        for (Value value : metricsSeries.getValues()) {
            series.add(value.getTimestamp(), round(value.asFloat()));
        }
        return series;
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
