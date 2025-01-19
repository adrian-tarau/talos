package net.microfalx.maven.extension;

import net.microfalx.lang.ArgumentUtils;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.PrintStream;

import static net.microfalx.lang.StringUtils.EMPTY_STRING;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

public class ProgressListener extends AbstractExecutionListener {

    private final MavenSession session;
    private final MavenConfiguration configuration;
    private final PrintStream output;

    public ProgressListener(MavenSession session, PrintStream output) {
        ArgumentUtils.requireNonNull(session);
        ArgumentUtils.requireNonNull(output);
        this.session = session;
        this.output = output;
        this.configuration = new MavenConfiguration(session);
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        if (!configuration.isProgress()) return;
        synchronized (output) {
            println();
            MavenProject project = event.getProject();
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(buffer().strong(project.getName()));
            buffer.append(' ');
            MavenUtils.appendDots(buffer);
            print(buffer.toString());
        }
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        projectEnded(event);
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        projectEnded(event);
    }

    public void projectEnded(ExecutionEvent event) {
        print(buffer().success("Done").toString());
    }

    void start() {
        if (!configuration.isProgress()) return;
        println();
        println(buffer().strong("Building "
                                + session.getTopLevelProject().getName() + " "
                                + session.getTopLevelProject().getVersion()).toString()
                + " ( " + MavenUtils.getRequestInfo(session) + ")");
    }

    private void println(String message) {
        output.println(message);
    }

    private void println() {
        println(EMPTY_STRING);
    }

    private void print(String message) {
        output.print(message);
    }


}
