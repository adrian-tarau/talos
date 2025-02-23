package net.microfalx.talos.report;

import net.microfalx.lang.ExceptionUtils;
import net.microfalx.resource.Resource;
import net.microfalx.talos.model.SessionMetrics;

import java.io.IOException;
import java.util.StringJoiner;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Builds an HTML report out of metrics of a Maven session.
 */
public class FragmentBuilder {

    private final Fragment fragment;
    private final SessionMetrics session;

    /**
     * Creates a new fragment builder.
     *
     * @param fragment the fragment
     * @param session  the metrics of a Maven session
     * @return a non-null instance
     */
    public static FragmentBuilder create(Fragment fragment, SessionMetrics session) {
        return new FragmentBuilder(fragment, session);
    }

    private FragmentBuilder(Fragment fragment, SessionMetrics session) {
        requireNonNull(fragment);
        requireNonNull(session);
        this.fragment = fragment;
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
        String name = fragment.getType().name().toLowerCase();
        fragment.content = resource;
        try {
            Template.create(name).setSession(session).setSelector(name).render(resource);
        } catch (Exception e) {
            fragment.throwable = e;
            ExceptionUtils.throwException(e);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FragmentBuilder.class.getSimpleName() + "[", "]")
                .add("fragment=" + fragment)
                .add("session=" + session)
                .toString();
    }
}
