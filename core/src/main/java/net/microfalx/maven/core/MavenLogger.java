package net.microfalx.maven.core;

import net.microfalx.lang.IOUtils;
import net.microfalx.resource.Resource;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.eclipse.sisu.Priority;
import org.joor.Reflect;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.impl.MavenSimpleLoggerFactory;
import org.slf4j.impl.SimpleLogger;
import org.slf4j.impl.SimpleLoggerConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ExceptionUtils.getRootCauseMessage;

/**
 * A logger which forwards the logging to a logger and also accumulates the messages
 * for a later use in console reports if Maven runs in quiet mode.
 * <p>
 * It also acts as a central point for logs.
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class MavenLogger extends AbstractMavenLifecycleParticipant {

    private static final String LOGGER_PREFIX = "build.";
    private static final int LOG_LEVEL_OFF = 50;

    private final org.slf4j.Logger logger;
    private final StringBuilder buffer = new StringBuilder(8000);

    private MavenConfiguration configuration;

    private PrintStream originalSystemOutputPrintStream;
    private Resource systemOutputResource;
    private PrintStream systemOutputPrintStream;

    private PrintStream originalSystemErrorPrintStream;
    private Resource systemErrorResource;
    private PrintStream systemErrorPrintStream;

    @Inject
    protected MavenSession session;

    public static MavenLogger create(Class<?> clazz) {
        return create(LoggerFactory.getLogger(clazz));
    }

    public static MavenLogger create(org.slf4j.Logger logger) {
        return new MavenLogger(logger);
    }

    public MavenLogger() {
        this(LoggerFactory.getLogger(MavenLogger.class));
        originalSystemOutputPrintStream = System.out;
        originalSystemErrorPrintStream = System.err;
    }

    public PrintStream getSystemOutputPrintStream() {
        return originalSystemOutputPrintStream;
    }
    
    public Resource getSystemOutput() {
        return systemOutputResource;
    }

    public Resource getSystemError() {
        return systemErrorResource;
    }

    private MavenLogger(Logger logger) {
        requireNonNull(logger);
        this.logger = logger;
    }

    public void debug(String message) {
        this.logger.debug(message);
        // append(message);
    }

    public void debug(String format, Object... arguments) {
        this.logger.debug(format, arguments);
        String message = MessageFormatter.arrayFormat(format, arguments).getMessage();
        //append(message);
    }

    public void info(String message) {
        this.logger.info(message);
        append(message);
    }

    public void info(String format, Object... arguments) {
        this.logger.info(format, arguments);
        String message = MessageFormatter.arrayFormat(format, arguments).getMessage();
        append(message);
    }

    public void warn(String message) {
        this.logger.warn(message);
        append(message);
    }

    public void warn(String format, Object... arguments) {
        this.logger.warn(format, arguments);
        String message = MessageFormatter.arrayFormat(format, arguments).getMessage();
        append(message);
    }

    public String getReport() {
        return buffer.toString();
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        super.afterSessionStart(session);
        configuration = new MavenConfiguration(session);
        initSystemStreams();
        initLogging();
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        super.afterSessionEnd(session);
        releaseSystemStreams();
    }

    private void append(String message) {
        buffer.append(message).append('\n');
    }

    private void initSystemStreams() {
        systemOutputResource = configuration.getStorageDirectory().resolve(LOGGER_PREFIX + "output.log", Resource.Type.FILE);
        systemErrorResource = configuration.getStorageDirectory().resolve(LOGGER_PREFIX + "error.log", Resource.Type.FILE);
        debug("Initialize loggers to " + systemOutputResource.toURI());
        try {
            OutputStream systemOutputResourceStream = systemOutputResource.getOutputStream();
            if (!configuration.isQuiet()) {
                systemOutputResourceStream = new TeeOutputStream(systemOutputResourceStream, originalSystemOutputPrintStream);
            }
            systemOutputPrintStream = new PrintStream(systemOutputResourceStream, true);
            System.setOut(systemOutputPrintStream);

            OutputStream systemErrorResourceStream = systemErrorResource.getOutputStream();
            if (!configuration.isQuiet()) {
                systemErrorResourceStream = new TeeOutputStream(systemErrorResourceStream, originalSystemErrorPrintStream);
            }
            systemErrorPrintStream = new PrintStream(systemErrorResourceStream, true);
            System.setErr(systemErrorPrintStream);
        } catch (IOException e) {
            warn("Failed to initialize system output stream, root cause: {}", getRootCauseMessage(e));
        }
    }

    private void releaseSystemStreams() {
        IOUtils.closeQuietly(systemOutputPrintStream);
        IOUtils.closeQuietly(systemErrorPrintStream);
    }

    void initLogging() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof MavenSimpleLoggerFactory)) return;
        SimpleLoggerConfiguration config = Reflect.onClass(SimpleLogger.class).get("CONFIG_PARAMS");
        Object outputChoice = Reflect.onClass("org.slf4j.impl.OutputChoice").create(systemOutputPrintStream).get();
        Reflect.on(config).set("outputChoice", outputChoice);
        if (!configuration.isQuiet()) return;
    }
}
