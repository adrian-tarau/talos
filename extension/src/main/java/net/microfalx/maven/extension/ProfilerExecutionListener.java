package net.microfalx.maven.extension;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public class ProfilerExecutionListener extends AbstractExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerExecutionListener.class);

    private final ProfilerMetrics profilerMetrics;

    public ProfilerExecutionListener(ProfilerMetrics profilerMetrics) {
        requireNonNull(profilerMetrics);
        this.profilerMetrics = profilerMetrics;
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        super.projectStarted(event);
        profilerMetrics.projectStart(event.getProject());
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        super.projectSucceeded(event);
        profilerMetrics.projectStop(event.getProject(), null);
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        super.projectFailed(event);
        profilerMetrics.projectStop(event.getProject(), event.getException());
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        profilerMetrics.projectStop(event.getProject(), null);
    }
}
