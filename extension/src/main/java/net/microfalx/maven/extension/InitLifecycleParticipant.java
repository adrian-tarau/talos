package net.microfalx.maven.extension;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.eclipse.sisu.Priority;
import org.joor.Reflect;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.MavenSimpleLoggerFactory;
import org.slf4j.impl.SimpleLogger;
import org.slf4j.impl.SimpleLoggerConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;

@Named("microfalx")
@Singleton
@Priority(Integer.MAX_VALUE)
public class InitLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitLifecycleParticipant.class);

    private static final String PROJECT_PACKAGE = "net.microfalx";

    protected static final int LOG_LEVEL_OFF = 50;
    private MavenConfiguration configuration;
    private ProgressListener progressListener;
    private boolean loggerDisabled;

    @Inject
    private ProfilerMetrics profilerMetrics;

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
        LOGGER.info("Initialize extension, verbose: {}, quiet: {}, progress: {}",
                configuration.isVerbose(), configuration.isQuiet(),configuration.isProgress());
        initLogging();
        registerListener(session);
        VirtualMachineMetrics.get().start();
        ServerMetrics.get().start();
    }

    private void registerListener(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        ChainedListener lifecycleListener = new ChainedListener(request.getExecutionListener());
        lifecycleListener.addChainListener(new ProfilerExecutionListener(profilerMetrics));
        progressListener = new ProgressListener(session);
        lifecycleListener.addChainListener(progressListener);
        request.setExecutionListener(lifecycleListener);
    }

    void initLogging() {
        if (!configuration.isQuiet()) return;
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof MavenSimpleLoggerFactory)) return;
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + PROJECT_PACKAGE, "info");
        try {
            SimpleLoggerConfiguration config = Reflect.onClass(SimpleLogger.class).get("CONFIG_PARAMS");
            Reflect.on(config).set("defaultLogLevel", LOG_LEVEL_OFF);
            Map<String, Logger> loggers = Reflect.on(loggerFactory).get("loggerMap");
            loggers.values().forEach(logger -> {
                if (!logger.getClass().getName().startsWith(PROJECT_PACKAGE)) {
                    Reflect.on(logger).set("currentLogLevel", LOG_LEVEL_OFF);
                }
            });
            loggerDisabled = true;
        } catch (Exception e) {
            // ignore any failure here
        }
    }

}
