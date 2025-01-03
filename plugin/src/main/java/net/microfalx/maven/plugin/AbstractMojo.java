package net.microfalx.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Base class for all Mojos.
 */
public abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String buildDirectory;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String projectVersion;

    @Parameter(defaultValue = "false", readonly = true, property = "dry_run")
    private boolean dryRun;

    @Parameter(defaultValue = "false", readonly = true, property = "debug")
    private boolean debug;

    @Component
    protected RepositorySystem repository;

    /**
     * Returns whether the execution of the Mojo should only simulate the execution.
     *
     * @return <code>true</code> if a dry run, <code>false</code> otherwise
     */
    protected final boolean isDryRun() {
        return dryRun;
    }

    /**
     * Returns whether additional information (for debug purposes) is logged to the console.
     *
     * @return <code>true</code> to log debug information, <code>false</code> otherwise
     */
    protected final boolean isDebug() {
        return debug;
    }

    /**
     * Returns the version of the project (module) running this task.
     * <p>
     * The project version is either a configuration injected with <code>projectVersion</code> property or the module
     * version.
     *
     * @return a non-null instance
     */
    protected final String getVersion() {
        if (projectVersion != null) {
            return projectVersion;
        } else {
            return project.getVersion();
        }
    }

    /**
     * Returns the build directory (target)
     *
     * @return a non-null instance
     */
    protected final File getBuildDirectory() {
        return new File(buildDirectory);
    }

    /**
     * Returns the top project.
     *
     * @return the project
     */
    protected final MavenProject getTopProject() {
        return session.getTopLevelProject();
    }

    /**
     * Returns a list with all modules, sorted in the execution order.
     *
     * @return a non-null instance
     */
    protected final List<MavenProject> getProjects() {
        return session.getProjectDependencyGraph().getSortedProjects();       
    }

    /**
     * Returns whether this project the last project in the reactor.
     *
     * @return @{code true} if last project (including only project), {@code false}
     */
    protected final boolean isLastProjectInReactor() {
        List<MavenProject> sortedProjects = getProjects();
        MavenProject lastProject = sortedProjects.isEmpty() ? session.getCurrentProject() : sortedProjects.getLast();
        return session.getCurrentProject().equals(lastProject);
    }

    /**
     * Returns the artifacts available to the project (module).
     *
     * @return a non-null set
     */
    protected final Set<Artifact> getArtifacts() {
        return project.getArtifacts();
    }
    
}
