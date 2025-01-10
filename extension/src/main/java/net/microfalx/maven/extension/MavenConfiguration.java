package net.microfalx.maven.extension;

import org.apache.maven.execution.MavenSession;

import java.time.Duration;

import static java.time.Duration.ofMillis;
import static net.microfalx.maven.core.MavenUtils.getProperty;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration extends net.microfalx.maven.core.MavenConfiguration {

    private Duration minimumDuration;
    private Boolean consoleEnabled;

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
     * Returns whether the console is enabled and should display reports and summaries.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    public Boolean isConsoleEnabled() {
        if (consoleEnabled == null) {
            consoleEnabled = getProperty(getSession(), "console.enabled", true);
        }
        return consoleEnabled;
    }
}
