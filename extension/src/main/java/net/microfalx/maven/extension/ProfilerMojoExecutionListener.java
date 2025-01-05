package net.microfalx.maven.extension;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.sisu.Priority;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named("mojo")
@Singleton
@Priority(1)
public class ProfilerMojoExecutionListener implements org.apache.maven.execution.MojoExecutionListener {

    @Inject
    private ProfilerMetrics profilerMetrics;

    @Override
    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
        profilerMetrics.mojoStarted(event.getMojo(), event.getExecution());
    }

    @Override
    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        profilerMetrics.mojoStop(event.getMojo(), null);
    }

    @Override
    public void afterExecutionFailure(MojoExecutionEvent event) {
        profilerMetrics.mojoStop(event.getMojo(), event.getCause());
    }
}
