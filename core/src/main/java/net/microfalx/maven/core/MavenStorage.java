package net.microfalx.maven.core;

import net.microfalx.lang.Hashing;
import net.microfalx.lang.JvmUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.resource.Resource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FileUtils.validateDirectoryExists;
import static net.microfalx.resource.Resource.Type.DIRECTORY;

/**
 * Provides locations to store files for Maven projects.
 */
public class MavenStorage {

    private static final String STORAGE_DIRECTORY = "maven";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Duration MAX_WORKSPACE_RETENTION = Duration.ofDays(3);


    private static Resource storageDirectory;
    private static Resource workspaceDirectory;
    private static Resource stagingDirectory;
    private static Resource trendDirectory;

    /**
     * Returns a director used to store files for any maven plugins.
     *
     * @return a non-null instance
     */
    public static synchronized Resource getStorageDirectory() {
        if (storageDirectory == null) {
            File directory;
            if (JvmUtils.isHomeWritable()) {
                directory = new File(JvmUtils.getCacheDirectory(), STORAGE_DIRECTORY);
            } else {
                directory = new File(new File(JvmUtils.getTemporaryDirectory(), JvmUtils.STORE_NAME), STORAGE_DIRECTORY);
            }
            storageDirectory = Resource.directory(validateDirectoryExists(directory));
        }
        return storageDirectory;
    }

    /**
     * Returns the directory to store data for all workspaces (mostly temporary data).
     *
     * @return a non-null instance
     */
    public static synchronized Resource getWorkspaceDirectory() {
        return getStorageDirectory().resolve("workspace", DIRECTORY);
    }

    /**
     * Returns a staging directory used to collect data related to a build.
     *
     * @return the staging directory for a session
     */
    public static synchronized Resource getStagingDirectory() {
        return getStorageDirectory().resolve("staging", DIRECTORY);
    }

    /**
     * Returns a staging directory used to collect data related to a build.
     *
     * @param session the session
     * @return the staging directory for a session
     */
    public static synchronized Resource getStagingDirectory(MavenSession session) {
        requireNonNull(session);
        if (stagingDirectory == null) {
            stagingDirectory = getStagingDirectory().resolve(getBuildId(session));
        }
        return stagingDirectory;
    }

    /**
     * Returns the directory to store data for a given session (mostly temporary data).
     *
     * @param session the session
     * @return a non-null instance
     */
    public static synchronized Resource getWorkspaceDirectory(MavenSession session) {
        requireNonNull(session);
        if (workspaceDirectory == null) {
            workspaceDirectory = getWorkspaceDirectory().resolve(getProjectId(session), DIRECTORY)
                    .resolve(getTimestampedName(), DIRECTORY);
        }
        return workspaceDirectory;
    }

    /**
     * Returns the directory to store data for trends a given project.
     *
     * @param session the session
     * @return a non-null instance
     */
    public static synchronized Resource getTrendsDirectory(MavenSession session) {
        requireNonNull(session);
        if (trendDirectory == null) {
            trendDirectory = getStorageDirectory().resolve("trends", DIRECTORY)
                    .resolve(getProjectId(session), DIRECTORY);
        }
        return trendDirectory;
    }

    /**
     * Stores trend metrics.
     *
     * @param session the session
     * @param trend   the trend resource
     * @throws IOException if an I/O error occurs
     */
    public static void storeTrend(MavenSession session, Resource trend) throws IOException {
        requireNonNull(session);
        requireNonNull(trend);
        String fileName = "trend_" + getTimestampedName() + ".data";
        getTrendsDirectory(session).resolve(fileName).copyFrom(trend);
    }

    /**
     * Returns a list of resources containing trends information.
     *
     * @param session the session
     * @return a non-null instance
     * @throws IOException if an I/O error occurs
     */
    public static Collection<Resource> getTrends(MavenSession session) throws IOException {
        return getTrendsDirectory(session).list();
    }

    /**
     * Cleanups the workspaces based on file age.
     */
    public static void cleanupWorkspace(MavenSession session) {
        requireNonNull(session);
        cleanupWorkspace(getWorkspaceDirectory());
        cleanupWorkspace(getStagingDirectory());
    }

    private static void cleanupWorkspace(Resource resource) {
        try {
            resource.walk((root, child) -> {
                if (isOld(child.lastModified(), child.isFile())) {
                    try {
                        child.delete();
                    } catch (IOException e) {
                        // ignore any failure during remove
                    }
                }
                return true;
            });
        } catch (Exception e) {
            // ignore any failure during remove
        }
    }

    private static String getTimestampedName() {
        return DATE_FORMATTER.format(LocalDateTime.now());
    }

    private static String getProjectId(MavenSession session) {
        if (session.getTopLevelProject() == null) {
            throw new IllegalArgumentException("The session does not have a project attached");
        }
        MavenProject project = session.getTopLevelProject();
        return project.getGroupId() + "." + project.getArtifactId();
    }

    private static String getBuildId(MavenSession session) {
        Hashing hashing = Hashing.create();
        hashing.update(session.getRequest().getBaseDirectory());
        return StringUtils.toIdentifier(getTimestampedName(), hashing.asString());
    }

    private static boolean isOld(long timestamp, boolean file) {
        Duration age = ofMillis(currentTimeMillis() - timestamp);
        return age.compareTo(file ? MAX_WORKSPACE_RETENTION : MAX_WORKSPACE_RETENTION.plusDays(1)) > 0;
    }
}
