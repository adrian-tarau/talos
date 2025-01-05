package net.microfalx.maven.extension;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

@Named("microfalx")
@Singleton
@Priority(1)
public class InitLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitLifecycleParticipant.class);

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        registerListener(session);
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        super.afterSessionStart(session);
        LOGGER.info("Initialize ");
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        LOGGER.info("After session end");
    }

    private void registerListener(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        ChainedListener lifecycleListener = new ChainedListener(request.getExecutionListener());
        lifecycleListener.addChainListener(new ProfilerExecutionListener());
        request.setExecutionListener(lifecycleListener);
    }
}
