package net.microfalx.boot;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static net.microfalx.boot.BootstrapUtils.*;

/**
 * Builds the application class path based on files discovered in ~/lib directory.
 */
public class ApplicationBuilder {

    private final Collection<File> files = new ArrayList<>();
    private File home;
    private File libDirectory;
    private File logsDirectory;
    private File tmpDirectory;

    public ApplicationBuilder() {
        initDirectories();
    }

    /**
     * Returns the home directory.
     *
     * @return a non-null instance
     */
    public File getHome() {
        return home;
    }

    /**
     * Changes the home directory.
     *
     * @param home a new home directory
     */
    public void setHome(File home) {
        requireNonNull(home);
        updateHome(home);
    }

    /**
     * Returns the directory containing the libraries.
     *
     * @return a non-null instance
     */
    public File getLibDirectory() {
        return libDirectory;
    }

    /**
     * Returns the logs directory.
     *
     * @return a non-null instance
     */
    public File getLogsDirectory() {
        return logsDirectory;
    }

    /**
     * Returns the temporary files directory.
     *
     * @return a non-null instance
     */
    public File getTmpDirectory() {
        return tmpDirectory;
    }

    /**
     * Returns the class path of the application.
     *
     * @return a non-null instance
     */
    public URL[] getClassPath() {
        discoverFiles();
        return files.stream().map(ApplicationBuilder::toUrl).collect(Collectors.toList()).toArray(new URL[0]);
    }

    private void discoverFiles() {
        files.clear();
        File[] jars = libDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        Bootstrap.get().log("Build class path from ''{0}''", libDirectory.getAbsolutePath());
        if (jars != null) {
            long totalSize = Arrays.stream(jars).mapToLong(File::length).sum();
            Bootstrap.get().log("Found {0} libraries, size {1}", jars.length, BootstrapUtils.formatBytes(totalSize));
            this.files.addAll(Arrays.asList(jars));
        }
    }

    private void initDirectories() {
        String home = Bootstrap.getSystemProperty("home");
        if (isNotEmpty(home)) {
            updateHome(new File(home));
        } else {
            home = System.getenv("HOME");
            if (isEmpty(home)) {
                throw new IllegalStateException("Environment variable HOME does not exist");
            }
            updateHome(new File(home));
        }
    }

    private void updateHome(File home) {
        requireNonNull(home);
        this.home = home;
        if (!this.home.exists()) {
            throw new IllegalStateException("Home directory '" + this.home + "' does not exist");
        }
        libDirectory = new File(home, "lib");
        if (!libDirectory.exists()) {
            throw new IllegalStateException("A directory with libraries (" + libDirectory + ") does not exist");
        }
        tmpDirectory = new File(home, "tmp");
        if (!tmpDirectory.exists()) {
            throw new IllegalStateException("A directory for temporary files (" + tmpDirectory + ") does not exist");
        }
        logsDirectory = new File(home, "logs");
        if (!logsDirectory.exists()) validateDirectoryExists(logsDirectory);
    }

    private static URL toUrl(final File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new BootstrapException(e);
        }
    }
}
