package net.microfalx.maven.extension;

import net.microfalx.lang.ArgumentUtils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

public class ProgressListener extends AbstractExecutionListener {

    private final MavenSession session;
    private final MavenConfiguration configuration;

    public ProgressListener(MavenSession session) {
        ArgumentUtils.requireNonNull(session);
        this.session = session;
        this.configuration = new MavenConfiguration(session);
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        super.projectStarted(event);
        if (!configuration.isProgress()) return;
        MavenProject project = event.getProject();
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(buffer().strong(project.getName()));
        buffer.append(' ');
        MavenUtils.appendDots(buffer);
        log(buffer.toString());
    }

    void start() {
        if (!configuration.isProgress()) return;
        log(buffer().strong("Build Progress for "
                            + session.getTopLevelProject().getName() + " "
                            + session.getTopLevelProject().getVersion()).toString());
    }

    private void log(String message) {
        System.out.println(message);
    }
}
