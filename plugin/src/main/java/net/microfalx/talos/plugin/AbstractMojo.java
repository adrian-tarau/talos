package net.microfalx.talos.plugin;

import com.google.common.primitives.Ints;
import net.microfalx.lang.ObjectUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.lang.Version;
import net.microfalx.resource.Resource;
import net.microfalx.talos.core.MavenStorage;
import org.apache.commons.lang3.math.NumberUtils;
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
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;
import static net.microfalx.lang.IOUtils.appendStream;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.talos.core.MavenUtils.*;

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

    @Parameter(defaultValue = "false", property = "talos.dry_run")
    private boolean dryRun;

    @Parameter(defaultValue = "false", property = "talos.debug")
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
        String buildNumber = System.getProperty(getBuildNumber(), Integer.toString(5 + ThreadLocalRandom.current().nextInt(10)));
        Version version = Version.parse(getVersionAsString());
        if (NumberUtils.isDigits(buildNumber)) {
            version = version.withBuild(Integer.parseInt(buildNumber));
        }
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
     * Returns the current project.
     *
     * @return a non-null instance
     */
    protected final MavenProject getProject() {
        return project;
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
     * Returns the Maven session.
     *
     * @return a non-null instance
     */
    protected final MavenSession getSession() {
        return session;
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
        MavenProject lastProject = sortedProjects.isEmpty() ? session.getCurrentProject() : sortedProjects.get(sortedProjects.size() - 1);
        return session.getCurrentProject().equals(lastProject);
    }

    /**
     * Returns the build timestamp as returned by the Maven BuildNumber plugin.
     * <p>
     * If the plugin is not enabled, it will be generated.
     *
     * @return a non-null instance
     */
    protected final String getBuildTime() {
        String buildTime = (String) getProperty(MAVEN_BUILD_TIMESTAMP_PROP);
        if (StringUtils.isEmpty(buildTime)) {
            String timestampPattern = defaultIfEmpty((String) getProperty(MAVEN_BUILD_TIMESTAMP_PATTERN_PROP), MAVEN_BUILD_TIMESTAMP_PATTERN_DEFAULT);
            buildTime = DateTimeFormatter.ofPattern(timestampPattern).format(LocalDateTime.now());
        }
        return buildTime;
    }

    /**
     * Returns the build hash (SCM commit) as returned by the Maven BuildNumber plugin.
     *
     * @return a non-null instance
     */
    protected final String getBuildHash() {
        return StringUtils.defaultIfEmpty((String) getProperty(MAVEN_BUILD_NUMBER_PROP), "na");
    }

    /**
     * Returns the build number for the current session.
     *
     * @return the build number as a string
     */
    protected final String getBuildNumber() {
        return getBuildNumber(1);
    }

    /**
     * Returns the build number for the current session.
     *
     * @return the build number as a string
     */
    protected final String getBuildNumber(Integer defaultValue) {
        if (defaultValue == null) defaultValue = 1;
        String sessionBuildNumber = StringUtils.trim((String) getSession().getUserProperties().get(CI_BUILD_NUMBER_PROP));
        if (isNotEmpty(sessionBuildNumber)) return sessionBuildNumber;
        String ciBuildNumber = defaultIfEmpty(getProject().getProperties().getProperty(CI_BUILD_NUMBER_PROP),
                getTopProject().getProperties().getProperty(CI_BUILD_NUMBER_PROP));
        ciBuildNumber = defaultIfEmpty(ciBuildNumber, System.getenv(CI_BUILD_NUMBER_PROP));
        sessionBuildNumber = emptyIfNull(ciBuildNumber);
        if (isEmpty(sessionBuildNumber)) {
            sessionBuildNumber = getBuildNumber(getTopProject().getArtifact(), defaultValue);
        }
        getSession().getUserProperties().put(CI_BUILD_NUMBER_PROP, sessionBuildNumber);
        return sessionBuildNumber;
    }

    /**
     * Returns the build number associated with an artifact.
     *
     * @param artifact the artifact
     * @return a positive integer
     */
    protected final String getBuildNumber(Artifact artifact, int defaultValue) {
        Resource buildNumbers = MavenStorage.getConfigurationDirectory().resolve("build_number", Resource.Type.DIRECTORY);
        Resource versionResource = buildNumbers.resolve(toIdentifier(artifact.getGroupId() + "_" + artifact.getArtifactId()));
        int version = defaultValue - 1;
        try {
            if (versionResource.exists()) {
                Integer storedVersion = Ints.tryParse(emptyIfNull(versionResource.loadAsString().trim()));
                if (storedVersion != null) {
                    version = storedVersion + 1;
                }
            }
            appendStream(versionResource.getWriter(), new StringReader(Integer.toString(version)));
        } catch (IOException e) {
            getLog().warn("Failed to extract build number from artifact " + artifact, e);
        }
        return Integer.toString(Math.max(1, version));
    }

    /**
     * Returns the artifacts available to the project (module).
     *
     * @return a non-null set
     */
    protected final Set<Artifact> getArtifacts() {
        return project.getArtifacts();
    }

    /**
     * Returns the property value by looking up into various project and session properties.
     *
     * @param name the property name
     * @return the value
     */
    protected final Object getProperty(String name) {
        Object value = getProject().getProperties().getProperty(name);
        if (ObjectUtils.isEmpty(value)) value = getTopProject().getProperties().getProperty(name);
        if (ObjectUtils.isEmpty(value)) value = getSession().getUserProperties().getProperty(name);
        if (ObjectUtils.isEmpty(value)) value = System.getProperty(name);
        return value;
    }

}
