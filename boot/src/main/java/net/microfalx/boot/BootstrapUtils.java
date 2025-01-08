package net.microfalx.boot;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Various utilities.
 */
public class BootstrapUtils {

    /**
     * Checks that the specified object reference is not {@code null}.
     *
     * @param value the object reference to check for nullity
     * @param <T>   the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws NullPointerException if {@code obj} is {@code null}
     */
    public static <T> T requireNonNull(T value) {
        if (value == null) throw new IllegalArgumentException("Argument cannot be NULL");
        return value;
    }

    /**
     * Formats a number of bytes.
     *
     * @param value the number
     * @return the formatted number
     */
    public static String formatBytes(long value) {
        return value / (1024 * 1024) + "MB";
    }

    /**
     * Returns whether the string is empty.
     *
     * @param value the string to validate
     * @return {@code true} if empty, @{code false} otherwise
     */
    public static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    /**
     * Returns whether the string is not empty.
     *
     * @param value the string to validate
     * @return {@code true} if empty, @{code false} otherwise
     */
    public static boolean isNotEmpty(CharSequence value) {
        return !isEmpty(value);
    }

    /**
     * Validated the directory, creates the directory if it does not exist and fails if the directory cannot be created.
     *
     * @param directory the directory
     * @return the directory, after validation
     */
    public static File validateDirectoryExists(File directory) {
        requireNonNull(directory);
        if (!directory.exists()) directory.mkdirs();
        if (!directory.exists()) {
            throwException(new IllegalStateException("Directory '" + directory.getAbsolutePath() + "' could not be created"));
        }
        return directory;
    }

    /**
     * Rethrow a checked exception
     *
     * @param exception an exception
     */
    @SuppressWarnings("SameReturnValue")
    public static <T> T throwException(Throwable exception) {
        doThrowException(exception);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void doThrowException(Throwable exception) throws E {
        requireNonNull(exception);
        throw (E) exception;
    }

    /**
     * Dumps an exception stacktrace as a string.
     *
     * @param throwable an exception
     * @return exception stacktrace, N/A if null
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) return "N/A";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
