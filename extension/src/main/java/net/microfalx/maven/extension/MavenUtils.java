package net.microfalx.maven.extension;

import java.time.Duration;

import static net.microfalx.lang.TimeUtils.NANOSECONDS_IN_MILLISECONDS;

/**
 * Various utilities around Maven.
 */
public class MavenUtils {

    static final int LONG_NAME_LENGTH = 60;
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
        }
        return builder;
    }


}
