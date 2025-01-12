package net.microfalx.maven.extension;

import net.microfalx.lang.ObjectUtils;
import net.microfalx.maven.core.MavenLogger;
import net.microfalx.maven.core.MavenUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Priority;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

@Named("mojo")
@Singleton
@Priority(1)
public class ProfilerMojoExecutionListener implements MojoExecutionListener {

    @Inject
    private ProfilerMetrics profilerMetrics;

    @Inject
    private MavenLogger logger;

    @Inject
    private MavenSession session;

    private MavenConfiguration configuration;
    private PrintStream output;

    private volatile MavenProject lastProject;
    private volatile String lastGoal;

    private static final Map<String, String> goalsToPrint = new HashMap<>();

    @Override
    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
        profilerMetrics.mojoStarted(event.getMojo(), event.getExecution());
        if (configuration.isQuietAndWithProgress()) printMojo(event);
    }

    @Override
    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        profilerMetrics.mojoStop(event.getMojo(), null);
    }

    @Override
    public void afterExecutionFailure(MojoExecutionEvent event) {
        profilerMetrics.mojoStop(event.getMojo(), event.getCause());
    }

    private void printMojo(MojoExecutionEvent event) {
        if (!configuration.isQuietAndWithProgress()) return;
        String goal = MavenUtils.getGoal(event.getExecution());
        if (!ObjectUtils.equals(lastGoal, goal)) {
            String action = goalsToPrint.get(goal);
            if (action != null) print(action);
        }
        print(".");
        lastGoal = goal;
        lastProject = event.getProject();
    }

    @PostConstruct
    public void initialize() {
        configuration = new MavenConfiguration(session);
        output = logger.getSystemOutputPrintStream();
    }

    private void print(String message) {
        output.print(message);
    }

    static {
        goalsToPrint.put("clean", "clean");
        goalsToPrint.put("compiler:compile", "compile");
        goalsToPrint.put("compiler:testCompile", "test");
        goalsToPrint.put("failsafe:verify", "verify");
        goalsToPrint.put("javadoc:jar", "doc");
        goalsToPrint.put("jar:jar", "package");
        goalsToPrint.put("install:install", "install");
        goalsToPrint.put("deploy:deploy", "deploy");
    }
}
