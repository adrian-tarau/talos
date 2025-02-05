package net.microfalx.maven.extension;

import net.microfalx.lang.StringUtils;
import net.microfalx.maven.core.MavenConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;

import java.time.Duration;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.COMMA_WITH_SPACE;
import static net.microfalx.lang.StringUtils.isNotEmpty;
import static net.microfalx.lang.TimeUtils.NANOSECONDS_IN_MILLISECONDS;

/**
 * Various utilities around Maven.
 */
public class MavenUtils {

    static final int LONG_NAME_LENGTH = 60;
    static final int MEDIUM_NAME_LENGTH = 50;
    static final int SHORT_NAME_LENGTH = 40;

    private static int indent;

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

    /**
     * Returns a string describing the most important request parameters for a session.
     *
     * @param session the session
     * @return the info
     */
    public static String getRequestInfo(MavenSession session) {
        net.microfalx.maven.core.MavenConfiguration configuration = new MavenConfiguration(session);
        StringBuilder builder = new StringBuilder();
        String profiles = getProfiles(session);
        if (isNotEmpty(profiles)) StringUtils.append(builder, "Profiles: " + profiles, COMMA_WITH_SPACE);
        String goals = getGoals(session);
        if (isNotEmpty(goals)) StringUtils.append(builder, "Goals: " + goals, COMMA_WITH_SPACE);
        if (session.getRequest().getDegreeOfConcurrency() > 0) {
            StringUtils.append(builder, "DOP: " + configuration.getDop(), COMMA_WITH_SPACE);
        }
        if (session.getRequest().isOffline()) StringUtils.append(builder, "Offline");
        return builder.toString();
    }

    /**
     * Converts an artifact to a dependency.
     *
     * @param artifact the project
     * @return a non-null instance
     */
    public static Dependency fromArtifact(Artifact artifact) {
        requireNonNull(artifact);
        Dependency dependency = new Dependency();
        dependency.setGroupId(artifact.getGroupId());
        dependency.setArtifactId(artifact.getArtifactId());
        dependency.setVersion(artifact.getVersion());
        return dependency;
    }

    /**
     * Returns the identifier of a dependency.
     *
     * @param dependency the artifact
     * @return a non-null instance
     */
    public static String getId(net.microfalx.maven.model.Dependency dependency) {
        requireNonNull(dependency);
        return StringUtils.toIdentifier(dependency.getGroupId() + ":" + dependency.getArtifactId());
    }

    /**
     * Returns the number of spaces to be used in console.
     *
     * @return a non-null instance
     */
    public static String getIndentSpaces() {
        return getIndentSpaces(indent);
    }

    /**
     * Returns the number of spaces to be used in console.
     *
     * @param indent the level of indentation
     * @return a non-null instance
     */
    public static String getIndentSpaces(int indent) {
        return INDENTS[Math.min(INDENTS.length - 1, indent)];
    }

    public static void increaseIndent() {
        indent++;
    }

    public static void decreaseIndent() {
        indent--;
        if (indent < 0) indent = 0;
    }

    private static String getGoals(MavenSession session) {
        return String.join(" ", session.getRequest().getGoals());
    }

    private static String getProfiles(MavenSession session) {
        return String.join(" ", session.getRequest().getActiveProfiles());
    }

    private static final String[] INDENTS = {"", "  ", "    "};


}
