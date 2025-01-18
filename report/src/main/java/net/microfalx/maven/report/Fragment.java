package net.microfalx.maven.report;

import net.microfalx.lang.EnumUtils;
import net.microfalx.lang.Identifiable;
import net.microfalx.lang.Nameable;
import net.microfalx.lang.StringUtils;
import net.microfalx.resource.Resource;

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
            case DEPENDENCIES:
                return "fa-solid fa-circle-nodes";
            case ENVIRONMENT:
                return "fa-solid fa-gauge";
            case LOG:
                return "fa-regular fa-file-lines";
            case FAILURE:
                return "fa-solid fa-triangle-exclamation";
            case PERFORMANCE:
                return "fa-solid fa-flag-checkered";
            case PLUGINS:
                return "fa-solid fa-plug";
            case PROJECTS:
                return "fa-solid fa-diagram-project";
            default:
                return "fa-solid fa-notdef";
        }
    }

    public Resource getContent() {
        return content;
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
        LOG,

        /**
         * The failure, if any
         */
        FAILURE,

        /**
         * The projects (modules) part of the session.
         */
        PROJECTS,

        /**
         * The dependencies of the project
         */
        DEPENDENCIES,

        /**
         * Which plugins were used (and their parameters)
         */
        PLUGINS,

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
