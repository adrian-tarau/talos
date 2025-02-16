package net.microfalx.talos.core;

import net.microfalx.lang.Hashing;
import net.microfalx.lang.JvmUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.lang.TimeUtils;
import net.microfalx.resource.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;

import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.FileUtils.validateDirectoryExists;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.lang.UriUtils.parseUri;
import static net.microfalx.resource.Resource.Type.DIRECTORY;

/**
 * Provides locations to store files for Maven projects.
 */
public class MavenStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenStorage.class);

    private static final String STORAGE_DIRECTORY = "maven";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Duration MAX_WORKSPACE_RETENTION = Duration.ofDays(3);

    private static final String TRENDS_DIRECTORY_NAME = "trends";
    private static final String SESSIONS_DIRECTORY_NAME = "sessions";
    private static final String STAGING_DIRECTORY_NAME = "staging";

    private static Resource storageDirectory;
    private static Resource sessionDirectory;
    private static Resource stagingDirectory;
    private static Resource trendDirectory;
    private static Resource remoteStorageDirectory;

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
    public static synchronized Resource getLocalSessionsDirectory() {
        return getStorageDirectory().resolve(SESSIONS_DIRECTORY_NAME, DIRECTORY);
    }

    /**
     * Returns a staging directory used to collect data related to a build.
     *
     * @return the staging directory for a session
     */
    public static synchronized Resource getStagingDirectory() {
        return getStorageDirectory().resolve(STAGING_DIRECTORY_NAME, DIRECTORY);
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
    public static synchronized Resource getLocalSessionsDirectory(MavenSession session) {
        requireNonNull(session);
        if (sessionDirectory == null) {
            sessionDirectory = getLocalSessionsDirectory().resolve(getProjectId(session), DIRECTORY)
                    .resolve(getTimestampedName(session), DIRECTORY);
        }
        return sessionDirectory;
    }

    /**
     * Returns the directory to store data for a given session (mostly temporary data).
     *
     * @param session the session
     * @return a non-null instance
     */
    public static synchronized Resource getRemoteSessionsDirectory(MavenSession session) {
        requireNonNull(session);
        return getRemoteStorage(session).resolve(SESSIONS_DIRECTORY_NAME, DIRECTORY)
                .resolve(getProjectId(session), DIRECTORY)
                .resolve(getTimestampedName(session), DIRECTORY);
    }

    /**
     * Returns the directory to store data for trends a given project.
     *
     * @param session the session
     * @return a non-null instance
     */
    public static synchronized Resource getLocalTrendsDirectory(MavenSession session) {
        requireNonNull(session);
        if (trendDirectory == null) {
            trendDirectory = getStorageDirectory().resolve(TRENDS_DIRECTORY_NAME, DIRECTORY)
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
    public static Resource storeTrend(MavenSession session, Resource trend) throws IOException {
        requireNonNull(session);
        requireNonNull(trend);
        String fileName = "trend_" + getTimestampedName(session) + ".data";
        Resource resource = getLocalTrendsDirectory(session).resolve(fileName);
        resource.copyFrom(trend);
        return resource;
    }

    /**
     * Uploads the trend metrics to a remote store.
     *
     * @param session the maven session
     * @param trend   the trend resource
     * @throws IOException if an I/O error occurs
     */
    public static void uploadTrend(MavenSession session, Resource trend) throws IOException {
        if (hasRemoteStorage(session)) {
            Resource remoteTrendsDirectory = getRemoteTrendsDirectory(session);
            Resource remoteTrend = remoteTrendsDirectory.resolve(trend.getFileName());
            remoteTrend.copyFrom(trend);
        }
    }

    /**
     * Returns a list of resources containing trends information.
     *
     * @param session the session
     * @return a non-null instance
     * @throws IOException if an I/O error occurs
     */
    public static Collection<Resource> getLocalTrends(MavenSession session) throws IOException {
        return getLocalTrendsDirectory(session).list();
    }

    /**
     * Returns a list of resources containing trends information.
     *
     * @param session the session
     * @return a non-null instance
     * @throws IOException if an I/O error occurs
     */
    public static Collection<Resource> getRemoteTrends(MavenSession session) throws IOException {
        return getRemoteTrendsDirectory(session).list();
    }

    /**
     * Returns whether a remote storage was configured.
     *
     * @param session the session
     * @return {@code true} if configured, {@code false} otherwise
     */
    public static boolean hasRemoteStorage(MavenSession session) {
        return ResourceUtils.exists(getRemoteStorage(session));
    }

    /**
     * Returns a resource used to store session data remotely.
     * <p>
     * If the remote store is not configured, the {@link Resource#NULL} is returned.
     *
     * @param session the session
     * @return a non-null instance
     */
    public static Resource getRemoteStorage(MavenSession session) {
        requireNonNull(session);
        if (remoteStorageDirectory == null) {
            String uri = MavenUtils.getProperty(session, "storage.uri", (String) null);
            if (isNotEmpty(uri)) {
                String userName = MavenUtils.getProperty(session, "storage.username", (String) null);
                String password = MavenUtils.getProperty(session, "storage.password", (String) null);
                Credential credential = Credential.NA;
                if (isNotEmpty(userName) && isNotEmpty(password)) {
                    credential = new UserPasswordCredential(userName, password);
                }
                LOGGER.info("Initialize remote storage, uri: {}, username {}", uri, defaultIfEmpty(userName, NA_STRING));
                String endpoint = null;
                String s3Bucket = MavenUtils.getProperty(session, "storage.s3.bucket", (String) null);
                String s3Prefix = MavenUtils.getProperty(session, "storage.s3.prefix", (String) null);
                if (isNotEmpty(s3Bucket)) {
                    endpoint = uri;
                    LOGGER.info("Use S3 bucket '{}', prefix {}", s3Bucket, defaultIfEmpty(s3Prefix, NA_STRING));
                    uri = "s3:/" + removeStartSlash(removeEndSlash(s3Bucket));
                    if (isNotEmpty(s3Prefix)) uri += "/" + removeStartSlash(removeEndSlash(s3Prefix));
                }
                remoteStorageDirectory = ResourceFactory.resolve(parseUri(uri), credential, DIRECTORY);
                if (endpoint != null) {
                    remoteStorageDirectory = remoteStorageDirectory.withAttribute(Resource.END_POINT_ATTR, endpoint);
                }
                boolean exist = ResourceUtils.exists(remoteStorageDirectory);
                if (!exist) {
                    LOGGER.error("Remote storage '{}', credential {} does not exist or cannot be accessed", remoteStorageDirectory, credential);
                }
            } else {
                remoteStorageDirectory = Resource.NULL;
            }
        }
        return remoteStorageDirectory;
    }

    /**
     * Returns the directory to store data for trends a given project.
     *
     * @param session the session
     * @return a non-null instance
     */
    public static synchronized Resource getRemoteTrendsDirectory(MavenSession session) {
        return getRemoteStorage(session).resolve(TRENDS_DIRECTORY_NAME, DIRECTORY)
                .resolve(getProjectId(session), DIRECTORY);
    }

    /**
     * Cleanups the workspaces based on file age.
     */
    public static void cleanupWorkspace(MavenSession session) {
        requireNonNull(session);
        cleanupWorkspace(getLocalSessionsDirectory());
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

    private static String getTimestampedName(MavenSession session) {
        Date date = session.getStartTime();
        LocalDateTime startTime = LocalDateTime.now();
        if (date != null) TimeUtils.toLocalDateTime(date);
        return DATE_FORMATTER.format(startTime);
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
        return StringUtils.toIdentifier(getTimestampedName(session), hashing.asString());
    }

    private static boolean isOld(long timestamp, boolean file) {
        Duration age = ofMillis(currentTimeMillis() - timestamp);
        return age.compareTo(file ? MAX_WORKSPACE_RETENTION : MAX_WORKSPACE_RETENTION.plusDays(1)) > 0;
    }
}
