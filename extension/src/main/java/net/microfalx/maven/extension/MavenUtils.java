package net.microfalx.maven.extension;

import net.microfalx.lang.StringUtils;
import net.microfalx.lang.TimeUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.lang.TimeUtils.MILLISECONDS_IN_SECOND;
import static net.microfalx.lang.TimeUtils.NANOSECONDS_IN_MILLISECONDS;

/**
 * Various utilities around Maven.
 */
public class MavenUtils {

    private static final Map<String, String> mojoNames = new ConcurrentHashMap<>();

    private static final int DURATION_LENGTH = 9;
    static final int LONG_NAME_LENGTH = 65;
    static final int MEDIUM_NAME_LENGTH = 50;
    static final int SHORT_NAME_LENGTH = 40;

    /**
     * Returns the duration between two relative points in time.
     *
     * @param start the start time in nanoseconds
     * @param end   the end time in nanoseconds
     * @return a non-null instance
     */
    public static Duration getDuration(long start, long end) {
        long duration = Math.max(end - start, 0) / NANOSECONDS_IN_MILLISECONDS;
        return Duration.ofMillis(duration);
    }

    /**
     * Append dots up to a maximum line length.
     *
     * @param builder the builder
     * @return the builder
     */
    public static StringBuilder appendDots(StringBuilder builder) {
        return appendDots(builder, LONG_NAME_LENGTH);
    }

    /**
     * Append dots up to a maximum line length.
     *
     * @param builder   the builder
     * @param maxLength the maximum length for dots
     * @return the builder
     */
    public static StringBuilder appendDots(StringBuilder builder, int maxLength) {
        if (builder.length() <= maxLength) {
            while (builder.length() < maxLength) {
                builder.append('.');
            }
            builder.append(' ');
        }
        return builder;
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
        StringBuilder builder = new StringBuilder();
        builder.append(" [");
        String durationAsString = formatDuration(duration.toMillis());
        int padSize = DURATION_LENGTH - durationAsString.length();
        if (padSize > 0) builder.append(getStringOfChar(' ', padSize));
        builder.append(durationAsString);
        builder.append(']');
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
        long d = duration / TimeUtils.ONE_DAY;
        String format;
        if (d > 0) {
            // Length 11+ chars
            format = "%d d %02d:%02d h";
        } else if (h > 0) {
            // Length 7 chars
            format = "%2$02d:%3$02d h";
        } else if (m > 0) {
            // Length 9 chars
            format = "%3$02d:%4$02d min";
        } else {
            // Length 7-8 chars
            format = "%4$d.%5$03d s";
        }
        return String.format(format, d, h, m, s, ms);
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
