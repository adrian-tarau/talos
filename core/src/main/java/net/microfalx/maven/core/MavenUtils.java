package net.microfalx.maven.core;

import net.microfalx.lang.*;
import net.microfalx.metrics.Metrics;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;
import static net.microfalx.lang.FormatterUtils.formatBytes;
import static net.microfalx.lang.FormatterUtils.formatPercent;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.lang.TimeUtils.MILLISECONDS_IN_SECOND;

/**
 * Various Maven utilities.
 */
public class MavenUtils {

    private static final String PROPERTY_PREFIX = "microfalx.";

    public static Metrics METRICS = Metrics.of("Maven");
    public static final String ZERO_DURATION = "~0s";

    private static final int DURATION_LENGTH = 9;
    private static final Map<String, String> mojoNames = new ConcurrentHashMap<>();

    /**
     * Returns whether a known logger to intercept is available.
     *
     * @return {@code true} if Maven logger, {@code false} otherwise
     */
    public static boolean isLoggerAvailable() {
        return isMavenLoggerAvailable();
    }

    /**
     * Returns whether the Maven logger is available.
     *
     * @return {@code true} if Maven logger, {@code false} otherwise
     */
    public static boolean isMavenLoggerAvailable() {
        return ClassUtils.exists("org.slf4j.impl.MavenSimpleLoggerFactory");
    }

    /**
     * Returns the identifier of an artifact.
     *
     * @param artifact the artifact
     * @return a non-null instance
     */
    public static String getId(Artifact artifact) {
        requireNonNull(artifact);
        return StringUtils.toIdentifier(artifact.getGroupId() + ":" + artifact.getArtifactId());
    }

    /**
     * Returns the identifier of an artifact.
     *
     * @param artifact the artifact
     * @return a non-null instance
     */
    public static String getId(org.apache.maven.artifact.Artifact artifact) {
        requireNonNull(artifact);
        return StringUtils.toIdentifier(artifact.getGroupId() + ":" + artifact.getArtifactId());
    }

    /**
     * Returns the identifier of a dependency.
     *
     * @param dependency the dependency
     * @return a non-null instance
     */
    public static String getId(Dependency dependency) {
        requireNonNull(dependency);
        return StringUtils.toIdentifier(dependency.getGroupId() + ":" + dependency.getArtifactId());
    }

    /**
     * Returns the identifier of a plugin.
     *
     * @param plugin the plugin
     * @return a non-null instance
     */
    public static String getId(Plugin plugin) {
        requireNonNull(plugin);
        return StringUtils.toIdentifier(plugin.getGroupId() + ":" + plugin.getArtifactId());
    }

    /**
     * Returns the identifier of a mojo.
     *
     * @param mojo the Mojo
     * @return a non-null instance
     */
    public static String getId(Mojo mojo) {
        requireNonNull(mojo);
        return StringUtils.toIdentifier(ClassUtils.getName(mojo));
    }

    /**
     * Returns the identifier of a project.
     *
     * @param project the project
     * @return a non-null instance
     */
    public static String getId(MavenProject project) {
        return StringUtils.toIdentifier(project.getGroupId() + ":" + project.getArtifactId());
    }

    /**
     * Returns the identifier of an artifact.
     *
     * @param metadata the artifact
     * @return a non-null instance
     */
    public static String getId(Metadata metadata) {
        return StringUtils.toIdentifier(metadata.getGroupId() + ":" + metadata.getArtifactId());
    }

    /**
     * Returns a formatted integer, left padded.
     *
     * @param value  the value to format
     * @param length the minimum length
     * @return a padded number
     */
    public static String formatInteger(int value, int length) {
        return formatInteger(value, length, true);
    }

    /**
     * Returns a formatted integer, left padded.
     *
     * @param value  the value to format
     * @param length the minimum length
     * @return a padded number
     */
    public static String formatInteger(int value, int length, boolean digits) {
        return leftPad(Integer.toString(value), length, digits ? '0' : ' ');
    }

    /**
     * Returns a left padded (with spaces) text.
     *
     * @param text   the text to pad
     * @param length the minimum length
     * @return a non null padded string
     */
    public static String leftPad(String text, int length) {
        return leftPad(text, length, ' ');
    }

    /**
     * Returns a left padded (with spaces) text.
     *
     * @param text   the text to pad
     * @param length the minimum length
     * @param fill   the character to be used to fill the empty space
     * @return a non null padded string
     */
    public static String leftPad(String text, int length, char fill) {
        text = defaultIfNull(text, EMPTY_STRING);
        if (text.length() < length) {
            text = StringUtils.getStringOfChar(fill, length - text.length()) + text;
        }
        return text;
    }

    /**
     * Formats duration Maven style.
     *
     * @param duration the duration
     * @return the duration as string
     */
    public static String formatDuration(Duration duration) {
        return formatDuration(duration, true, true);
    }

    /**
     * Formats duration Maven style.
     *
     * @param duration the duration
     * @return the duration as string
     */
    public static String formatDuration(Duration duration, boolean brackets) {
        return formatDuration(duration, brackets, true);
    }

    /**
     * Formats duration Maven style.
     *
     * @param duration the duration
     * @return the duration as string
     */
    public static String formatDuration(Duration duration, boolean brackets, boolean padding) {
        StringBuilder builder = new StringBuilder();
        if (brackets) builder.append("[");
        String durationAsString = duration.toMillis() == 0 ? ZERO_DURATION : formatDuration(duration.toMillis());
        if (padding) {
            int padSize = DURATION_LENGTH - durationAsString.length();
            if (padSize > 0) builder.append(getStringOfChar(' ', padSize));
        }
        builder.append(durationAsString);
        if (brackets) builder.append(']');
        return builder.toString();
    }

    /**
     * Formats duration Maven style.
     *
     * @param duration the duration
     * @return the duration as string
     */
    public static String formatDuration(long duration) {
        long ms = duration % 1000;
        long s = (duration / MILLISECONDS_IN_SECOND) % 60;
        long m = (duration / TimeUtils.ONE_MINUTE) % 60;
        long h = (duration / TimeUtils.ONE_HOUR) % 24;
        String format;
        if (h > 0) {
            // Length 7 chars
            format = "%1$02d:%2$02dh";
        } else if (m > 0) {
            // Length 9 chars
            format = "%2$02d:%3$02dm";
        } else {
            // Length 7-8 chars
            format = "%3$d.%4$03ds";
        }
        return String.format(format, h, m, s, ms);
    }

    /**
     * Formats memory usage as a string.
     *
     * @param used    the used memory
     * @param maximum the maximum memory
     * @return a non-null instance
     */
    public static String formatMemory(long used, long maximum) {
        return formatBytes(used) + " of " + formatBytes(maximum)
               + " (" + formatPercent(NumberUtils.percent(used, maximum)) + ")";
    }

    /**
     * Returns a property value as a Duration.
     *
     * @param session      the session
     * @param name         the property name
     * @param defaultValue the default value
     * @return the value
     * @see TimeUtils#parseDuration(String)
     */
    public static Duration getProperty(MavenSession session, String name, Duration defaultValue) {
        String value = getProperty(session, name, (String) null);
        try {
            if (StringUtils.isNotEmpty(value)) return TimeUtils.parseDuration(value);
        } catch (NumberFormatException e) {
            // ignore and fall back to default value
        }
        return defaultValue;
    }

    /**
     * Returns a property value as a boolean.
     *
     * @param session      the session
     * @param name         the property name
     * @param defaultValue the default value
     * @return the value
     */
    public static boolean getProperty(MavenSession session, String name, boolean defaultValue) {
        String value = getProperty(session, name, (String) null);
        try {
            if (StringUtils.isNotEmpty(value)) return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            // ignore and fall back to default value
        }
        return defaultValue;
    }

    /**
     * Returns a property value as an integer.
     *
     * @param session      the session
     * @param name         the property name
     * @param defaultValue the default value
     * @return the value
     */
    public static int getProperty(MavenSession session, String name, int defaultValue) {
        String value = getProperty(session, name, (String) null);
        try {
            if (StringUtils.isNotEmpty(value)) return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // ignore and fall back to default value
        }
        return defaultValue;
    }

    /**
     * Returns a property value.
     *
     * @param session      the session
     * @param name         the property name
     * @param defaultValue the default value
     * @return the value
     */
    public static String getProperty(MavenSession session, String name, String defaultValue) {
        requireNonNull(session);
        requireNonNull(name);
        name = PROPERTY_PREFIX + name;
        String value = session.getSystemProperties().getProperty(name);
        if (isNotEmpty(value)) return value;
        MavenProject project = session.getCurrentProject();
        if (project == null) project = session.getTopLevelProject();
        if (project != null) value = ObjectUtils.toString(project.getProperties().get(name));
        if (isNotEmpty(value)) return value;
        return defaultValue;
    }

    /**
     * Registers a name for a Mojo.
     *
     * @param mojoClass the Mojo class.
     * @param name      the name
     */
    public static void registerName(String mojoClass, String name) {
        requireNotEmpty(mojoClass);
        requireNotEmpty(name);
        mojoNames.put(mojoClass.toLowerCase(), name);
    }

    /**
     * Returns the name of the Mojo.
     *
     * @param mojo the mojo
     * @return a non-null string
     */
    public static String getName(Mojo mojo) {
        requireNonNull(mojo);
        String name = mojoNames.get(mojo.getClass().getName().toLowerCase());
        if (name != null) return name;
        name = replaceFirst(mojo.getClass().getSimpleName(), "Mojo", EMPTY_STRING);
        return beautifyCamelCase(name);
    }

    /**
     * Returns the goal executed for a given Mojo.
     *
     * @param execution the mojo execution
     * @return a non-null string
     */
    public static String getGoal(MojoExecution execution) {
        MojoDescriptor descriptor = execution.getMojoDescriptor();
        String prefix = descriptor.getPluginDescriptor().getGoalPrefix();
        if (StringUtils.isEmpty(prefix)) {
            Plugin plugin = execution.getPlugin();
            prefix = plugin.getGroupId() + ":" + plugin.getArtifactId();
        }
        return prefix + ':' + execution.getGoal();
    }


    static {
        registerName("org.jacoco.maven.AgentMojo", "Jacoco Agent");
        registerName("org.jacoco.maven.ReportMojo", "Jacoco Report");
        registerName("org.apache.maven.plugin.resources.remote.ProcessRemoteResourcesMojo", "Remote Resources");
        registerName("org.apache.maven.plugin.surefire.SurefireMojo", "Unit Tests");
    }
}
