package net.microfalx.maven.report;

import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.lang.StringUtils;
import net.microfalx.maven.model.SessionMetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FormatterUtils.formatNumber;
import static net.microfalx.lang.StringUtils.COMMA_WITH_SPACE;
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

    public static <V extends Number> String getSeries(Iterable<V> series) {
        StringBuilder builder = new StringBuilder();
        for (V value : series) {
            StringUtils.append(builder, toString(value), COMMA_WITH_SPACE);
        }
        builder.insert(0, "[");
        builder.append("]");
        return builder.toString();
    }

    public static String getLabels(Iterable<String> labels) {
        StringBuilder builder = new StringBuilder();
        for (String label : labels) {
            label = "'" + label + "'";
            StringUtils.append(builder, label, COMMA_WITH_SPACE);
        }
        builder.insert(0, "[");
        builder.append("]");
        return builder.toString();
    }

    private static <V extends Number> String toString(V value) {
        if (value == null) {
            return "0";
        } else {
            return formatNumber(value, 2, EMPTY_STRING);
        }
    }

    public static class Json {

        private final String value;

        public Json(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static class Data<N extends Number> {

        private String x;
        private N y;

        public Data(String x, N y) {
            requireNonNull(x);
            this.x = x;
            this.y = y;
        }

        public String getX() {
            return x;
        }

        public N getY() {
            return y;
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

    public static class Chart extends NamedIdentityAware<String> {

        private Integer width;
        private Integer height;
        private Legend legend = new Legend();

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
    }

    public static abstract class DataChart<N extends Number> extends Chart {

        private Collection<Data<N>> data = new ArrayList<>();

        public DataChart(String id, String name) {
            super(id, name);
        }

        public Collection<Data<N>> getData() {
            List<Data<N>> sorted = new ArrayList<>(data);
            sorted.sort((o1, o2) -> -Double.compare(o1.getY().doubleValue(), o2.getY().doubleValue()));
            return unmodifiableCollection(sorted);
        }

        public DataChart<N> add(String x, N y) {
            requireNonNull(x);
            data.add(new Data<>(x, y));
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

    public static class TreeMapChart<N extends Number> extends DataChart<N> {

        public TreeMapChart(String id, String name) {
            super(id, name);
        }
    }
}
