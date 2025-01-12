package net.microfalx.maven.report;

import net.microfalx.maven.model.SessionMetrics;

import java.io.IOException;
import java.io.Writer;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Builds an HTML report out of metrics of a Maven session.
 */
public class FragmentBuilder {

    private final Type type;
    private final SessionMetrics session;

    /**
     * Creates a new fragment builder.
     *
     * @param type    the type of fragment
     * @param session the metrics of a Maven session
     * @return a non-null instance
     */
    public static FragmentBuilder create(Type type, SessionMetrics session) {
        return new FragmentBuilder(type, session);
    }

    private FragmentBuilder(Type type, SessionMetrics session) {
        requireNonNull(type);
        requireNonNull(session);
        this.type = type;
        this.session = session;
    }

    /**
     * Renders the template for this fragment.
     *
     * @param writer the writer
     * @throws IOException if an I/O error occurs
     */
    public void build(Writer writer) throws IOException {
        String name = type.name().toLowerCase();
        Template.create(name).setSession(session).setSelector(name).render(writer);
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
         * Performance metrics (CPU, Memory, etc)
         */
        PERFORMANCE,

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
         * Information about the team
         */
        TEAM,

        /**
         * Information about environment (OS, Server, Processes)
         */
        ENVIRONMENT
    }
}
