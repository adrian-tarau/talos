package net.microfalx.maven.docker;

/**
 * Base class for all image related exceptions.
 */
public class ImageException extends RuntimeException{

    public ImageException(String message) {
        super(message);
    }

    public ImageException(String message, Throwable cause) {
        super(message, cause);
    }
}
