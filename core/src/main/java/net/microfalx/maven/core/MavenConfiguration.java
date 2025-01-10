package net.microfalx.maven.core;

import org.apache.maven.execution.MavenSession;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.maven.core.MavenUtils.getProperty;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration {

    private static final int VERBOSE_NONE = 0;
    private static final int VERBOSE_SOME = 1;
    private static final int VERBOSE_MORE = 2;
    private static final int VERBOSE_ALL = 3;

    private Boolean verbose;
    private Boolean quiet;
    private Boolean progress;
    private Integer verboseLevel;

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
     * Returns whether the output in the terminal should be completely quiet.
     *
     * @return {@code true} to disable all logging, {@code false} otherwise
     */
    public final boolean isQuiet() {
        if (quiet == null) quiet = getProperty(session, "quiet", false);
        return isProgress() || quiet;
    }

    /**
     * Returns whether the output in the terminal should be verbose.
     *
     * @return {@code true} to be verbose, {@code false} otherwise
     */
    public final boolean isVerbose() {
        if (verbose == null) initVerbose();
        return verbose;
    }

    /**
     * Returns whether the build should show the progress report.
     *
     * @return {@code true} to be verbose, {@code false} otherwise
     */
    public final boolean isProgress() {
        if (progress == null) initProgress();
        return progress;
    }

    private void initVerbose() {
        verbose = getProperty(session, "verbose", false);
        verboseLevel = getProperty(session, "verboseLevel", VERBOSE_NONE);
        if (verbose) verboseLevel = VERBOSE_ALL;
    }

    private void initProgress() {
        progress = getProperty(session, "progress", false);
    }

}
