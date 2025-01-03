package net.microfalx.boot;

/**
 * Base class for all bootstrap exception.
 */
public class BootstrapException extends RuntimeException {

    public BootstrapException(String message) {
        super(message);
    }

    public BootstrapException(String message, Throwable cause) {
        super(message, cause);
    }

    public BootstrapException(Throwable cause) {
        super(cause);
    }
}
