package net.microfalx.maven.report;

import net.microfalx.lang.*;
import net.microfalx.resource.Resource;

import java.io.IOException;
import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Identifies a fragment in the report.
 */
public class Fragment implements Identifiable<String>, Nameable {

    private final String id;
    private final Type type;
    Resource content;

    public static Fragment create(Type type) {
        return new Fragment(type);
    }

    private Fragment(Type type) {
        requireNonNull(type);
        this.type = type;
        this.id = StringUtils.toIdentifier(type.name());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return EnumUtils.toLabel(type);
    }

    public Type getType() {
        return type;
    }

    public String getIcon() {
        switch (type) {
            case SUMMARY:
                return "fa-solid fa-list-check";
            case ARTIFACTS:
                return "fa-solid fa-circle-nodes";
            case ENVIRONMENT:
                return "fa-solid fa-gauge";
            case LOGS:
                return "fa-regular fa-file-lines";
            case FAILURE:
                return "fa-solid fa-triangle-exclamation";
            case PERFORMANCE:
                return "fa-solid fa-flag-checkered";
            case PLUGINS:
                return "fa-solid fa-plug";
            case PROJECT:
                return "fa-solid fa-diagram-project";
            case TESTS:
                return "fa-solid fa-clipboard-check";
            default:
                return "fa-solid fa-notdef";
        }
    }

    public Resource getResource() {
        return content;
    }

    public String getContent() {
        try {
            return content.loadAsString();
        } catch (IOException e) {
            return "<div class=\"alert alert-primary\" role=\"alert\">\n" +
                   "Failed to load fragment " + type + ", root cause: " + ExceptionUtils.getRootCauseMessage(e) +
                   "    </div>";
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Fragment.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("content=" + content.getName())
                .toString();
    }

    /**
     * An enum which identifies which section (fragment) of the report will be rendered
     */
    public enum Type {

        /**
         * Generic information about session
         */
        SUMMARY,

        /**
         * The log produced by the build
         */
        LOGS,

        /**
         * The failure, if any
         */
        FAILURE,

        /**
         * The project information.
         */
        PROJECT,

        /**
         * The artifacts of the project
         */
        ARTIFACTS,

        /**
         * Which plugins were used (and their parameters)
         */
        PLUGINS,

        /**
         * Executed tests
         */
        TESTS,

        /**
         * Performance metrics (CPU, Memory, etc)
         */
        PERFORMANCE,

        /**
         * Information about environment (OS, Server, Processes)
         */
        ENVIRONMENT
    }
}
