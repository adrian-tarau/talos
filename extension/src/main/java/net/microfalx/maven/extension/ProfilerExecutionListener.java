package net.microfalx.maven.extension;

import org.apache.maven.execution.AbstractExecutionListener;
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


}
