package net.microfalx.maven.extension;

import org.apache.maven.execution.MavenSession;

import java.time.Duration;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration {

    private Boolean verbose;
    private Duration minimumDuration;

    private final MavenSession session;

    public MavenConfiguration(MavenSession session) {
        requireNonNull(session);
        this.session = session;
    }

    public boolean isVerbose() {
        if (verbose == null) verbose = MavenUtils.getProperty(session, "verbose", false);
        return verbose;
    }

    public Duration getMinimumDuration() {
        if (minimumDuration == null)
            minimumDuration = MavenUtils.getProperty(session, "minimumDuration", Duration.ofMillis(100));
        return minimumDuration;
    }
}
