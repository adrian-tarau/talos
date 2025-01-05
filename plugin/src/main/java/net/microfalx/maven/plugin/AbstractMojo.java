package net.microfalx.maven.plugin;

import net.microfalx.lang.Version;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;

/**
 * Base class for all Mojos.
 */
public abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor pluginDescriptor;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String buildDirectory;

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String projectVersion;

    @Parameter(defaultValue = "false", readonly = true, property = "microfalx.dry_run")
    private boolean dryRun;

    @Parameter(defaultValue = "false", readonly = true, property = "microfalx.debug")
    private boolean debug;

    @Component
    private SecDispatcher securityDispatcher;

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
    protected final String getVersionAsString() {
        if (projectVersion != null) {
            return projectVersion;
        } else {
            return project.getVersion();
        }
    }

    /**
     * Returns the version of the project (module) running this task which includes the build
     * number.
     *
     * @return a non-null instance
     * @see #getVersion()
     */
    protected final Version getVersion() {
        // Extract build number from CI-provided property
        // If CI did not build this, default to 0 to indicate a local build
        String buildNumber = System.getProperty("build.number", Integer.toString(5 + ThreadLocalRandom.current().nextInt(10)));
        Version version = Version.parse(getVersionAsString());
        version = version.withBuild(Integer.parseInt(buildNumber));
        return version;
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
     * Returns the server with a given identifier.
     *
     * @param ids the server identifiers.
     * @return the server, null if it does not exist
     */
    protected final Server getServer(String... ids) {
        requireNotEmpty(ids);
        Server server = null;
        for (String id : ids) {
            server = session.getSettings().getServer(id.toLowerCase());
            if (server != null) break;
        }
        if (securityDispatcher != null && server != null) {
            try {
                server.setPassword(securityDispatcher.decrypt(server.getPassword()));
            } catch (SecDispatcherException e) {
                throw new SecurityException("Cannot decrypt password for server " + server.getId(), e);
            }
        }
        return server;
    }

    /**
     * Returns the plugin descriptor owning this Mojo.
     *
     * @return a non-null instance
     */
    protected final PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    /**
     * Returns the plugin owning this Mojo.
     *
     * @return a non-null instance
     */
    protected final Plugin getPlugin() {
        return pluginDescriptor.getPlugin();
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
