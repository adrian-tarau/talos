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
}
