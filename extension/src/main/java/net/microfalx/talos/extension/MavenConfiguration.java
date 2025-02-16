package net.microfalx.talos.extension;

import net.microfalx.lang.TimeUtils;
import org.apache.maven.execution.MavenSession;

import java.time.Duration;

import static java.time.Duration.ofMillis;
import static net.microfalx.talos.core.MavenUtils.getProperty;
import static net.microfalx.talos.core.MavenUtils.isMavenLoggerAvailable;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration extends net.microfalx.talos.core.MavenConfiguration {

    private Duration minimumDuration;
    private Boolean extensionEnabled;
    private Boolean performanceEnabled;

    public MavenConfiguration(MavenSession session) {
        super(session);
    }

    /**
     * Returns the minimum duration for a task to be a candidate for visualization.
     *
     * @return a non-null instance
     */
    public final Duration getMinimumDuration() {
        if (minimumDuration == null) {
            minimumDuration = getProperty(getSession(), "minimumDuration", ofMillis(100));
        }
        return minimumDuration;
    }

    /**
     * Returns whether the console report is enabled and should display reports and summaries.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isReportConsoleEnabled() {
        return getProperty(getSession(), "report.console.enabled", true) && !isMavenQuiet();
    }

    /**
     * Returns whether the HTML report is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isReportHtmlEnabled() {
        return getProperty(getSession(), "report.html.enabled", true) && !isMavenQuiet();
    }

    /**
     * Returns whether the logs should be included.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isReportLogsEnabled() {
        return getProperty(getSession(), "report.logs.enabled", true) && isMavenLoggerAvailable();
    }

    /**
     * Returns whether the HTML report is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Duration getTrendRetention() {
        String property = getProperty(getSession(), "report.trend.retention", "30d");
        return TimeUtils.parseDuration(property);
    }

    /**
     * Returns whether the trend report will only contain one entry for each day.
     *
     * @return {@code true} to include one per day, {@code false} to include all
     */
    public boolean isTrendReportingDaily() {
        return getProperty(getSession(), "report.trend.daily", true);
    }

    /**
     * Returns whether the performance tracking is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isPerformanceEnabled() {
        if (performanceEnabled == null) {
            performanceEnabled = getProperty(getSession(), "extension.performance.enabled", true);
        }
        return isExtensionEnabled() && performanceEnabled;
    }

    /**
     * Returns whether the extension is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isExtensionEnabled() {
        if (extensionEnabled == null) {
            extensionEnabled = getProperty(getSession(), "extension.enabled", true);
        }
        return extensionEnabled;
    }

    /**
     * Returns whether the extension is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isOpenReportEnabled() {
        return getProperty(getSession(), "report.open", false);
    }

    /**
     * Returns whether the environment report is enabled (in the terminal).
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isEnvironmentEnabled() {
        return getProperty(getSession(), "report.environment.enabled", false);
    }
}
