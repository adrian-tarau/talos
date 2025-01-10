package net.microfalx.maven.extension;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named("microfalx")
@Singleton
@Priority(Integer.MIN_VALUE)
public class InitLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitLifecycleParticipant.class);

    @Inject
    private ProfilerMetrics profilerMetrics;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        registerListener(session);
        VirtualMachineMetrics.get().start();
        ServerMetrics.get().start();
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        profilerMetrics.sessionStart();
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        profilerMetrics.sessionsEnd();
        profilerMetrics.print();
    }

    private void registerListener(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        ChainedListener lifecycleListener = new ChainedListener(request.getExecutionListener());
        lifecycleListener.addChainListener(new ProfilerExecutionListener(profilerMetrics));
        request.setExecutionListener(lifecycleListener);
    }
}
