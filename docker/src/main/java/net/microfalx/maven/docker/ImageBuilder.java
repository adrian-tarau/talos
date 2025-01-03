package net.microfalx.maven.docker;

import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.lang.ObjectUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.resource.ClassPathResource;
import org.apache.commons.io.FileUtils;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.ProgressHandler;
import org.mandas.docker.client.builder.DockerClientBuilder;
import org.mandas.docker.client.exceptions.DockerCertificateException;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ImageInfo;
import org.mandas.docker.client.messages.ProgressMessage;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.ArgumentUtils.requireNotEmpty;
import static net.microfalx.lang.FileUtils.*;
import static net.microfalx.lang.IOUtils.*;
import static net.microfalx.lang.JvmUtils.getTemporaryDirectory;
import static net.microfalx.lang.StringUtils.*;
import static net.microfalx.lang.TimeUtils.toLocalDateTime;

/**
 * An image builder using Docker.
 */
public final class ImageBuilder extends NamedIdentityAware<String> {

    private static final String RUN_CMD = "RUN set -eux && ";
    private static final String STAGING_PATH = "staging";
    private static final String DEFAULT_TAG = "latest";
    private static final int RUN_AS_USER = 1000;
    private static final int RUN_AS_GROUP = 3000;

    private Path directory;
    private String tag;
    private String image = "eclipse-temurin:21-jre-jammy";
    private boolean pull = true;
    private boolean base;
    private String packages;
    private String maintainer;
    private final Map<String, String> environment = new HashMap<>();
    private String mainClass;
    private final List<String> arguments = new ArrayList<>();
    private final List<File> libraries = new ArrayList<>();

    private final StringBuilder builder = new StringBuilder();
    private Path appPath = Paths.get("/opt/microfalx");
    private String appUser = "microfalx";
    private final Path appBinPath = appPath.resolve("bin");
    private final Path appConfigPath = appPath.resolve("config");
    private final Path appLibPath = appPath.resolve("lib");
    private final Path appVarPath = Paths.get("/var" + appPath);
    private final Path appVarConfigPath = appVarPath.resolve("config");
    private File workspaceDirectory;
    private File stagingDirectory;

    private ProgressHandlerLogger dockerLogger = new ProgressHandlerLogger();

    public ImageBuilder(String name) {
        this(name, DEFAULT_TAG);
    }

    public ImageBuilder(String name, String tag) {
        requireNotEmpty(name);
        requireNotEmpty(tag);
        setName(name);
        this.tag = tag;
    }

    /**
     * Returns the workspace directory.
     *
     * @return the workspace, null if one will be generated
     */
    public File getWorkspaceDirectory() {
        return workspaceDirectory;
    }

    /**
     * Changes the workspace directory.
     *
     * @param workspaceDirectory the new workspace directory
     */
    public void setWorkspaceDirectory(File workspaceDirectory) {
        this.workspaceDirectory = workspaceDirectory;
    }

    /**
     * Returns the path in the file system where files will be placed to be added to the image.
     *
     * @return a non-null instance
     */
    public Path getDirectory() {
        return directory;
    }

    /**
     * Changes the path in the file system where files will be placed to be added to the image.
     *
     * @param directory the path
     * @return self
     */
    public ImageBuilder setDirectory(Path directory) {
        this.directory = directory;
        return this;
    }

    /**
     * Returns the path where the application will be deployed (packages) inside the container.
     *
     * @return a non-null instance
     */
    public Path getPath() {
        return appPath;
    }

    /**
     * Changes the path where the application will be deployed (packages) inside the container.
     *
     * @param path the path
     */
    public ImageBuilder setPath(Path path) {
        requireNonNull(path);
        this.appPath = path;
        return this;
    }

    /**
     * Returns the tag associated with the image.
     * <p>
     * By default, it uses <code>latest</code>. Additional tags can be added to the
     *
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Returns the OS user which will own the application files.
     *
     * @return a non-null instance
     */
    public String getUser() {
        return appUser;
    }

    /**
     * Changes the OS user which will own the application files.
     *
     * @param user the user
     */
    public ImageBuilder setUser(String user) {
        requireNotEmpty(user);
        this.appUser = user;
        return this;
    }

    /**
     * Returns a collection with libraries used by the application.
     * <p>
     * The libraries will be deployed using {@link #appLibPath}
     *
     * @return a non-null instance
     */
    public Collection<File> getLibraries() {
        return unmodifiableCollection(libraries);
    }

    /**
     * Adds a new library.
     *
     * @param path the path to the library
     * @return self
     */
    public ImageBuilder addLibrary(File path) {
        requireNonNull(path);
        this.libraries.add(path);
        return this;
    }

    /**
     * Returns whether the image is the base image for applications.
     *
     * @return {@code true} if base image, {@code false} otherwise
     */
    public boolean isBase() {
        return base;
    }

    /**
     * Changes whether the image is the base image for applications.
     *
     * @param base {@code true} if base image, {@code false} otherwise
     */
    public ImageBuilder setBase(boolean base) {
        this.base = base;
        return this;
    }

    /**
     * Returns the base image.
     *
     * @return a non-null instance
     */
    public String getImage() {
        return image;
    }

    /**
     * Changes the base image.
     *
     * @param image the base image
     * @return self
     */
    public ImageBuilder setImage(String image) {
        requireNotEmpty(image);
        this.image = image;
        return this;
    }

    /**
     * Returns whether it should always attempt to pull a newer version of the image.
     *
     * @return <code>true</code> to always pool a newer image, <code>false</code> otherwise
     */
    public boolean isPull() {
        return pull;
    }

    /**
     * Changes whether it should always attempt to pull a newer version of the image.
     *
     * @param pull <code>true</code> to always pool a newer image, <code>false</code> otherwise
     * @return self
     */
    public ImageBuilder setPull(boolean pull) {
        this.pull = pull;
        return this;
    }

    /**
     * Returns the maintainer of the image (usually an email address).
     *
     * @return the maintainer, null if not provided.
     */
    public String getMaintainer() {
        return maintainer;
    }

    /**
     * Changes the maintainer of the image.
     *
     * @param maintainer the maintainer
     */
    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    /**
     * Returns additional packages to be installed in the image.
     *
     * @return a non-null instance if packages are provided, null otherwise
     */
    public String getPackages() {
        return packages;
    }

    /**
     * Returns a list of additional packages to be installed in the image.
     *
     * @param packages the packages
     * @return self
     */
    public ImageBuilder setPackages(String packages) {
        this.packages = packages;
        return this;
    }


    /**
     * Returns the environment variables.
     *
     * @return a non-null instance
     */
    public Map<String, String> getEnvironment() {
        return unmodifiableMap(environment);
    }

    /**
     * Registers a new environment variable.
     *
     * @param name  the name
     * @param value the value
     * @return self
     */
    public ImageBuilder addEnvironment(String name, String value) {
        requireNotEmpty(name);
        environment.put(name, value);
        return this;
    }

    /**
     * Returns the main class.
     *
     * @return a non-null instance
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Registers the main class.
     * <p>
     * The command will be created based on the main class and optional arguments.
     *
     * @param mainClass the name
     * @return self
     */
    public ImageBuilder setMainClass(String mainClass) {
        requireNotEmpty(mainClass);
        this.mainClass = mainClass;
        return this;
    }

    /**
     * Returns the arguments passed to the application.
     *
     * @return a non-null instance
     */
    public List<String> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    /**
     * Adds an argument for the application.
     *
     * @param value the value of the argument
     * @return self
     */
    public ImageBuilder addArgument(String value) {
        requireNotEmpty(value);
        this.arguments.add(value);
        return this;
    }

    /**
     * Builds the Docker configuration used to build the image.
     *
     * @return a non-null instance
     */
    public String buildDescriptor() {
        builder.setLength(0);
        initStagingArea();
        appendFrom();
        appendInstallPackages();
        appendInstallExtraPackages();
        appendEnvironment();
        appendMetadata();
        appendPrepareOs();
        try {
            appendFiles();
        } catch (IOException e) {
            throw new ImageException("Failed to prepare files for image " + getName());
        }
        appendAppEnv();
        appendEntryPoint();
        appendHealthChecks();
        return builder.toString();
    }

    /**
     * Builds an image using the current docker instance (DOCKER_HOST environment variable).
     *
     * @return the image info
     */
    public Image build() {
        writeDescriptor();
        try (DockerClient client = createClient()) {
            String id;
            try {
                id = client.build(workspaceDirectory.toPath(), getFullName(), dockerLogger, getBuildOptions());
            } catch (Exception e) {
                throw new ImageException("Failed to build image '" + getFullName() + "' using " + client.getHost()
                        + ", output:\n" + dockerLogger.getOutput(), e);
            } finally {
                try {
                    FileUtils.cleanDirectory(workspaceDirectory);
                } catch (Exception e) {
                    // ignore
                }
            }
            String imageWithDigest = getFullName("@" + id);
            try {
                return convert(client.inspectImage(imageWithDigest));
            } catch (Exception e) {
                throw new ImageException("Failed to extract image '" + imageWithDigest + "' using " + client.getHost()
                        + e);
            }
        }
    }

    private void writeDescriptor() {
        String descriptor = buildDescriptor();
        try {
            appendStream(getBufferedWriter(new File(workspaceDirectory, "Dockerfile")), new StringReader(descriptor));
        } catch (IOException e) {
            ExceptionUtils.throwException(e);
        }
    }

    private void initStagingArea() {
        if (workspaceDirectory == null) {
            workspaceDirectory = getTemporaryDirectory("microfalx", "docker");
        }
        stagingDirectory = validateDirectoryExists(new File(workspaceDirectory, STAGING_PATH));
    }

    private void appendFrom() {
        builder.append("FROM ").append(image).append("\n");
        appendLabel("net.microfalx.image.base", image);
    }

    private void appendInstallPackages() {
        if (!base) return;
        builder.append(RUN_CMD).append("apt-get update && apt-get -y install curl iputils-ping net-tools iproute2 ksh vim-tiny dnsutils htop dstat ssh-client")
                .append(" && rm -rf /var/lib/apt/lists/*\n");
    }

    private void appendInstallExtraPackages() {
        String extraPackages = defaultIfNull(packages, EMPTY_STRING);
        if (StringUtils.isEmpty(extraPackages)) return;
        builder.append(RUN_CMD).append("apt-get update && apt-get -y install ").append(extraPackages).append(" && rm -rf /var/lib/apt/lists/*\n");
    }

    private void appendEnvironment() {
        if (environment.isEmpty()) return;
        environment.put("BUILD_TIME", LocalDateTime.now().toString());
        builder.append("ENV");
        for (Map.Entry<String, String> entry : new TreeMap<>(environment).entrySet()) {
            builder.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
        builder.append('\n');
    }

    private void appendPrepareOs() {
        if (!base) return;
        builder.append(RUN_CMD).append("addgroup --gid ").append(RUN_AS_GROUP).append(" ").append(appUser)
                .append(" && adduser --disabled-login --uid ").append(RUN_AS_USER).append(" --ingroup ")
                .append(appUser).append(" --home ").append(toUnix(appPath)).append(' ').append(appUser);
        builder.append(" && chmod +rx ").append(toUnix(appPath))
                .append(" && mkdir -p ").append(toUnix(appVarPath))
                .append(" && mkdir -p ").append(toUnix(appVarPath.resolve("logs")))
                .append(" && mkdir -p ").append(toUnix(appVarPath.resolve("tmp")))
                .append(" && ln -s ").append(toUnix(appVarPath.resolve("logs"))).append(" ").append(toUnix(appPath.resolve("logs")))
                .append(" && ln -s ").append(toUnix(appVarPath.resolve("tmp"))).append(" ").append(toUnix(appPath.resolve("tmp")))
                .append(" && chown ").append(getOwner()).append(' ').append(toUnix(appPath.resolve("logs")))
                .append(" && chown ").append(getOwner()).append(' ').append(toUnix(appPath.resolve("tmp")))
                .append(" && mkdir -p ").append(toUnix(appConfigPath))
                .append(" && mkdir -p ").append(toUnix(appVarConfigPath))
                .append(" && chown -R ").append(getOwner()).append(' ').append(toUnix(appVarPath));
        builder.append('\n');
        appendEnvironment("HOME", toUnix(appPath));
        appendEnvironment("PATH", toUnix(appBinPath) + ":${PATH}");
        builder.append("VOLUME ").append(toUnix(appVarPath)).append("\n");
        builder.append("USER ").append(getOwner()).append("\n");
    }

    private void appendAppEnv() {
        if (!base && isNotEmpty(mainClass)) appendEnvironment("APP_MAIN_CLASS", mainClass);
    }

    private void appendEntryPoint() {
        if (!base) return;
        builder.append("CMD [").append("\"start\"").append("]\n");
        builder.append("ENTRYPOINT [");
        appendQuoted(toUnix(appBinPath.resolve("initd")));
        builder.append("]\n");
    }

    private void appendHealthChecks() {
        if (!base) return;
        builder.append("HEALTHCHECK  --interval=30s --timeout=10s --start-period=30m --retries=5")
                .append(" CMD curl --fail http://localhost:8080/status || exit 1").append("\n");
    }

    private void appendFiles() throws IOException {
        appendCore();
        appendLibs();
        applyPermissions();
        builder.append("COPY --chown=").append(getOwner()).append(" ").append(STAGING_PATH).append("/ ").append(toUnix(appPath)).append("/\n");
    }

    private void appendLibs() throws IOException {
        File libDirectory = validateDirectoryExists(new File(stagingDirectory, "lib"));
        for (File library : libraries) {
            File target = new File(libDirectory, library.getName());
            Files.copy(library.toPath(), target.toPath());
        }
    }

    private void appendCore() throws IOException {
        if (!base) return;
        ClassPathResource.directory("boot").walk((root, child) -> {
            String path = removeStartSlash(child.getPath(root));
            if (child.isFile()) {
                File target = validateFileExists(new File(stagingDirectory, path));
                appendStream(getBufferedOutputStream(target), child.getInputStream());
            }
            return true;
        });
    }

    private void applyPermissions() {
        File bin = new File(stagingDirectory, "bin");
        File[] files = bin.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.setExecutable(true, false)) {
                    throw new ImageException("Failed to make file '" + file + "' executable");
                }
            }
        }
    }

    private void appendMetadata() {
        if (StringUtils.isEmpty(maintainer)) return;
        appendLabel("maintainer", maintainer);
    }

    private void appendEnvironment(String name, String value) {
        builder.append("ENV ").append(name).append("=\"").append(value).append("\"\n");
    }

    private void appendLabel(String name, String value) {
        builder.append("LABEL ").append(name).append("=\"").append(value).append("\"\n");
    }

    private void appendQuoted(String value) {
        builder.append("\"").append(value).append("\"");
    }

    private String getOwner() {
        return appUser + ":" + appUser;
    }

    private DockerClient createClient() {
        try {
            return DockerClientBuilder.fromEnv().build();
        } catch (DockerCertificateException e) {
            return ExceptionUtils.throwException(e);
        }
    }

    private DockerClient.BuildParam[] getBuildOptions() {
        Collection<DockerClient.BuildParam> dockerOptions = new ArrayList<>();
        if (!pull) {
            dockerOptions.add(DockerClient.BuildParam.pullNewerImage());
        }
        dockerOptions.add(DockerClient.BuildParam.forceRm());
        dockerOptions.add(DockerClient.BuildParam.rm());
        return dockerOptions.toArray(new DockerClient.BuildParam[0]);
    }

    private Image convert(ImageInfo info) {
        Image.Builder imageBuilder = new Image.Builder(info.id());
        imageBuilder.architecture(info.architecture()).os(info.os())
                .size(info.size()).virtualSize(info.virtualSize())
                .createdAt(toLocalDateTime(info.created()))
                .author(info.author()).name(info.author()).description(info.comment());
        return imageBuilder.build();
    }

    private String getFullName() {
        return getFullName(null);
    }

    private String getFullName(String tag) {
        if (tag == null) tag = this.tag;
        return getName() + ":" + tag;
    }

    private static class ProgressHandlerLogger implements ProgressHandler {

        private final StringBuilder logger = new StringBuilder();
        private String lastMessage;

        public String getOutput() {
            return logger.toString();
        }

        @Override
        public void progress(ProgressMessage message) throws DockerException {
            if (StringUtils.isNotEmpty(message.error())) {
                if (!ObjectUtils.equals(lastMessage, message.error())) {
                    log(message.error(), "Error");
                    lastMessage = message.error();
                }
            } else if (StringUtils.isNotEmpty(message.stream())) {
                if (!ObjectUtils.equals(lastMessage, message.stream())) {
                    log(message.error(), null);
                    lastMessage = message.stream();
                }
            } else if (StringUtils.isNotEmpty(message.status())) {
                if (!ObjectUtils.equals(lastMessage, message.status())) {
                    log(message.error(), null);
                    lastMessage = message.status();
                }
            }
        }

        private void log(String message, String level) {
            if (level != null) {
                logger.append(level).append(": ");
            }
            logger.append(message).append('\n');
        }
    }


}
