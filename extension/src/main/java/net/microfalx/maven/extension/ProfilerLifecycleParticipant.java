package net.microfalx.maven.extension;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.maven.core.MavenLogger;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named("microfalx")
@Singleton
@Priority(Integer.MAX_VALUE)
public class ProfilerLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerLifecycleParticipant.class);

    private MavenConfiguration configuration;
    private ProgressListener progressListener;

    @Inject
    private ProfilerMetrics profilerMetrics;

    @Inject
    private TransferMetrics transferMetrics;

    @Inject
    private MavenLogger mavenLogger;

    @Inject
    private ProfilerMojoExecutionListener mojoExecutionListener;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        progressListener.start();
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        initialize(session);
        profilerMetrics.sessionStart();
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        profilerMetrics.sessionsEnd();
        profilerMetrics.print();
    }

    private void initialize(MavenSession session) {
        configuration = new MavenConfiguration(session);
        LOGGER.debug("Initialize extension, verbose: {}, quiet: {}, progress: {}, performance: {}",
                configuration.isVerbose(), configuration.isQuiet(), configuration.isProgress(),
                configuration.isPerformanceEnabled()
        );
        registerListeners(session);
        VirtualMachineMetrics.get().start();
        ServerMetrics.get().start();
    }

    private void registerListeners(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        ChainedListener lifecycleListener = new ChainedListener(request.getExecutionListener());
        request.setExecutionListener(lifecycleListener);
        // intercepts the activity with the repository
        request.setTransferListener(transferMetrics.intercept(request.getTransferListener()));
        RepositorySystemSession repositorySession = session.getRepositorySession();
        if (repositorySession instanceof DefaultRepositorySystemSession) {
            ((DefaultRepositorySystemSession) repositorySession).setTransferListener(transferMetrics);
        }
        // intercepts lifecycle of Maven for performance metrics
        lifecycleListener.addChainListener(new ProfilerExecutionListener(profilerMetrics));
        // displays build progress
        progressListener = new ProgressListener(session, mavenLogger.getSystemOutputPrintStream());
        lifecycleListener.addChainListener(progressListener);


    }


}
