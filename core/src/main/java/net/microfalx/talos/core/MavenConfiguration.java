package net.microfalx.talos.core;

import net.microfalx.lang.StringUtils;
import net.microfalx.resource.Resource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.File;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FileUtils.validateDirectoryExists;
import static net.microfalx.lang.FileUtils.validateFileExists;
import static net.microfalx.lang.StringUtils.isNotEmpty;
import static net.microfalx.talos.core.MavenUtils.getProperty;
import static net.microfalx.talos.core.MavenUtils.isMavenLoggerAvailable;

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
    private Integer verbosityLevel;

    private final MavenSession session;

    public MavenConfiguration(MavenSession session) {
        requireNonNull(session);
        this.session = session;
        initVerboseGoals();
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
        if (quiet == null) {
            if (isMavenQuiet()) {
                quiet = true;
            } else {
                Boolean quietOverride = getQuietOverride();
                if (quietOverride != null) {
                    quiet = quietOverride;
                } else {
                    quiet = getProperty(session, "quiet", true);
                }
            }
        }
        return quiet;
    }

    /**
     * Returns whether Maven was asked to build without any output.
     *
     * @return {@code true} if no output, {@code false} otherwise
     */
    public boolean isMavenQuiet() {
        return session.getRequest().getLoggingLevel() >= 3;
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
     * Returns whether the goals requested expect the output of Maven to have the usual verbosity.
     *
     * @return {@code true} to shaw default Maven logging, {@code false} otherwise
     */
    public boolean isVerboseGoals() {
        return MavenUtils.isVerboseGoal(getSession().getGoals());
    }

    /**
     * Returns whether the build should show the progress report.
     *
     * @return {@code true} to be verbose, {@code false} otherwise
     */
    public final boolean isProgress() {
        if (progress == null) progress = getProperty(session, "progress", true);
        return progress && isQuiet() && isMavenLoggerAvailable() && !isMavenQuiet();
    }

    /**
     * Returns whether the build is quiet and progress was requested.
     *
     * @return {@code true} if quiet with progress, {@code false} otherwise
     */
    public boolean isQuietAndWithProgress() {
        return isQuiet() && isProgress();
    }

    /**
     * Returns whether the build uses multiple threads to build the project.
     *
     * @return {@code true} if parallel, {@code false} otherwise
     */
    public boolean isParallel() {
        return getDop() > 1;
    }

    /**
     * Returns the degree of parallelism.
     *
     * @return a positiver integer
     */
    public int getDop() {
        return session.getRequest().getDegreeOfConcurrency();
    }


    /**
     * Returns a director inside the target directory.
     *
     * @param name     the name of the directory, null to get the target directory
     * @param topLevel {@code true} to select the top level target directory, {@code false} for the target directory
     *                 of the current project
     * @return a non-null instance
     */
    public Resource getTargetDirectory(String name, boolean topLevel) {
        return Resource.directory(validateDirectoryExists(getTargetReference(name, topLevel)));
    }

    /**
     * Returns a file inside the target directory.
     *
     * @param name     the name of the file
     * @param topLevel {@code true} to select the top level target directory, {@code false} for the target directory
     *                 of the current project
     * @return a non-null instance
     */
    public Resource getTargetFile(String name, boolean topLevel) {
        return Resource.file(validateFileExists(getTargetReference(name, topLevel)));
    }

    /**
     * Returns a director inside the target directory.
     *
     * @param project the project
     * @param name    the name of the directory
     * @return a non-null instance
     */
    public Resource getTargetDirectory(MavenProject project, String name) {
        return Resource.directory(validateDirectoryExists(getTargetReference(project, name)));
    }

    /**
     * Returns a file inside the target directory.
     *
     * @param project the project
     * @param name    the name of the file
     * @return a non-null instance
     */
    public Resource getTargetFile(MavenProject project, String name) {
        return Resource.file(validateFileExists(getTargetReference(project, name)));
    }

    /**
     * Subclasses can override the quiet mode.
     *
     * @return {@code true} to be quiet, {@code false} for verbose, {@code NULL} if there is no override
     */
    protected Boolean getQuietOverride() {
        return isVerboseGoals() ? false : null;
    }

    private File getTargetReference(String name, boolean topLevel) {
        File reference;
        if (topLevel) {
            MavenProject project = session.getTopLevelProject();
            if (project != null) {
                reference = getTargetReference(project, name);
            } else {
                return isNotEmpty(name) ? new File(getTargetFromRequest(), name) : getTargetFromRequest();
            }
        } else {
            MavenProject project = session.getCurrentProject();
            if (project != null) {
                reference = getTargetReference(project, name);
            } else {
                throw new IllegalArgumentException("A target reference (" + name + ") cannot retrieved since project information is not available");
            }
        }
        return reference;
    }

    private File getTargetReference(MavenProject project, String name) {
        if (StringUtils.isEmpty(name)) {
            return new File(project.getBuild().getDirectory());
        } else {
            return new File(project.getBuild().getDirectory(), name);
        }
    }

    private File getTargetFromRequest() {
        File baseDirectory = new File(session.getRequest().getBaseDirectory());
        return new File(baseDirectory, "target");
    }

    private void initVerbose() {
        verbose = getProperty(session, "verbose", false);
        verbosityLevel = getProperty(session, "verboseLevel", VERBOSE_NONE);
        if (verbose) verbosityLevel = VERBOSE_ALL;
    }

    private void initVerboseGoals() {
        String[] goals = StringUtils.split(getProperty(getSession(), "verbose.goals", (String) null), ",");
        for (String goal : goals) {
            MavenUtils.registerVerboseGoal(goal);
        }
    }


}
