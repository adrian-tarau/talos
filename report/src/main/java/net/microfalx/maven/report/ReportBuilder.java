package net.microfalx.maven.report;

import net.microfalx.maven.model.SessionMetrics;
import net.microfalx.resource.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Builds an HTML report out of metrics of a Maven session.
 */
public class ReportBuilder {

    private final SessionMetrics session;

    public static ReportBuilder create(SessionMetrics session) {
        return new ReportBuilder(session);
    }

    private ReportBuilder(SessionMetrics session) {
        this.session = session;
    }

    /**
     * Renders the template for this fragment.
     *
     * @param resource the resource
     * @throws IOException if an I/O error occurs
     */
    public void build(Resource resource) throws IOException {
        requireNonNull(resource);
        Collection<Fragment> fragments = buildFragments();
        Template.create("report")
                .setSession(session).addVariable("fragments", fragments)
                .render(resource);
    }

    private Collection<Fragment> buildFragments() throws IOException {
        Collection<Fragment> fragments = new ArrayList<>();
        for (Fragment.Type type : Fragment.Type.values()) {
            Fragment fragment = Fragment.create(type);
            Resource temporary = Resource.temporary("maven_report_" + type.name().toLowerCase() + "_", ".html");
            FragmentBuilder.create(fragment, session).build(temporary);
            fragments.add(fragment);
        }
        return fragments;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportBuilder.class.getSimpleName() + "[", "]")
                .add("session=" + session)
                .toString();
    }
}
