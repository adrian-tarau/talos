package net.microfalx.maven.core;

import org.apache.maven.execution.MavenSession;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.maven.core.MavenUtils.getProperty;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration {

    private Boolean verbose;

    private final MavenSession session;

    public MavenConfiguration(MavenSession session) {
        requireNonNull(session);
        this.session = session;
    }

    /**
     * Returns the session.
     *
     * @return a non-null instance
     */
    public final MavenSession getSession() {
        return session;
    }

    /**
     * Returns whether the output in the terminal should be verbose.
     *
     * @return {@code true} to be verbose, {@code false} otherwise
     */
    public final boolean isVerbose() {
        if (verbose == null) verbose = getProperty(session, "verbose", false);
        return verbose;
    }

}
