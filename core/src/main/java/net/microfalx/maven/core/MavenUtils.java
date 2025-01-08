package net.microfalx.maven.core;

import net.microfalx.lang.ObjectUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.lang.TimeUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.time.Duration;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.isNotEmpty;

/**
 * Various Maven utilities.
 */
public class MavenUtils {

    private static final String PROPERTY_PREFIX = "microfalx.";

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
}
