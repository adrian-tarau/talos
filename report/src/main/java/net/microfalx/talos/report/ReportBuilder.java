package net.microfalx.talos.report;

import net.microfalx.resource.Resource;
import net.microfalx.talos.model.SessionMetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import static java.util.Collections.unmodifiableCollection;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.rethrowException;

/**
 * Builds an HTML report out of metrics of a Maven session.
 */
public class ReportBuilder {

    private final SessionMetrics session;
    private boolean failOnError;
    private final Collection<Fragment> fragments = new ArrayList<>();

    public static ReportBuilder create(SessionMetrics session) {
        return new ReportBuilder(session);
    }

    private ReportBuilder(SessionMetrics session) {
        this.session = session;
    }

    /**
     * Returns a collection of exceptions encountered during report generation.
     *
     * @return a non-null instance
     */
    public Collection<Fragment> getFragments() {
        return unmodifiableCollection(fragments);
    }

    /**
     * Changes whether the report build should fail due to an error in one of the fragments.
     *
     * @param failOnError <code>true</code> to fail the report on
     */
    public ReportBuilder setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
        return this;
    }

    /**
     * Renders the template for this fragment.
     *
     * @param resource the resource
     * @throws IOException if an I/O error occurs
     */
    public void build(Resource resource) throws IOException {
        requireNonNull(resource);
        buildFragments();
        Template template = Template.create("report").setSession(session);
        template.addVariable("fragments", fragments);
        try {
            template.render(resource);
        } finally {
            cleanup();
        }
    }

    private void buildFragments() throws IOException {
        for (Fragment.Type type : Fragment.Type.values()) {
            Fragment fragment = Fragment.create(type);
            fragments.add(fragment);
            Resource temporary = Resource.temporary("talos_report_" + type.name().toLowerCase() + "_", ".html");
            try {
                FragmentBuilder.create(fragment, session).build(temporary);
            } catch (Exception e) {
                if (failOnError) rethrowException(e);
            }
        }
    }

    private void cleanup() {
        for (Fragment fragment : fragments) {
            if (fragment.getResource() != null) {
                try {
                    fragment.getResource().delete();
                } catch (Exception e) {
                    // not important
                }
            }
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportBuilder.class.getSimpleName() + "[", "]")
                .add("session=" + session)
                .toString();
    }
}
