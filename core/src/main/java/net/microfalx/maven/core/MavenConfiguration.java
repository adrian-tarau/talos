package net.microfalx.maven.core;

import net.microfalx.resource.Resource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.File;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FileUtils.validateDirectoryExists;
import static net.microfalx.lang.FileUtils.validateFileExists;
import static net.microfalx.maven.core.MavenUtils.getProperty;

/**
 * Resolves various Maven related configuration.
 */
public class MavenConfiguration {

    private static final int VERBOSE_NONE = 0;
    private static final int VERBOSE_SOME = 1;
    private static final int VERBOSE_MORE = 2;
    private static final int VERBOSE_ALL = 3;

    private static final String STORAGE_DIRECTORY = ".microfalx";

    private Boolean verbose;
    private Boolean quiet;
    private Boolean progress;
    private Integer verbosityLevel;

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
        if (quiet == null) quiet = getProperty(session, "quiet", true);
        return quiet;
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
        if (progress == null) progress = getProperty(session, "progress", true);
        return progress && isQuiet();
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
     * Returns a director used to store files for any maven plugins.
     *
     * @return a non-null instance
     */
    public Resource getStorageDirectory() {
        File baseDirectory = new File(session.getRequest().getBaseDirectory());
        return Resource.directory(validateDirectoryExists(new File(baseDirectory, STORAGE_DIRECTORY)));
    }

    /**
     * Returns a director inside the target directory.
     *
     * @param name     the name of the directory
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

    private File getTargetReference(String name, boolean topLevel) {
        File reference;
        if (topLevel) {
            MavenProject project = session.getTopLevelProject();
            if (project != null) {
                reference = getTargetReference(project, name);
            } else {
                return new File(getTargetFromRequest(), name);
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

    private File getTargetReference(MavenProject project, String fileName) {
        return new File(project.getBuild().getDirectory(), fileName);
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

    private void initProgress() {
        ;
    }

}
