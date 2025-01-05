package net.microfalx.maven.extension;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfilerExecutionListener extends AbstractExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerExecutionListener.class);

    @Override
    public void mojoStarted(ExecutionEvent event) {
        //LOGGER.info("Mojo started: " + event.getMojoExecution());
    }

}
