package net.microfalx.maven.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * A logger which forwards the logging to a logger and also accumulates the messages
 * for a later use in console reports if Maven runs in quiet mode.
 */
public class MavenLogger {

    private final org.slf4j.Logger logger;
    private final StringBuilder buffer = new StringBuilder(8000);

    public static MavenLogger create(Class<?> clazz) {
        return create(LoggerFactory.getLogger(clazz));
    }

    public static MavenLogger create(org.slf4j.Logger logger) {
        return new MavenLogger(logger);
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

    public String getReport() {
        return buffer.toString();
    }

    private void append(String message) {
        buffer.append(message).append('\n');
    }
}
