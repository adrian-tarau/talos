package net.microfalx.maven.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.resource.Resource;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;
import static net.microfalx.lang.StringUtils.NA_STRING;

/**
 * Holds metrics about a Maven session.
 */
public class SessionMetrics extends NamedIdentityAware<String> {

    private static final int SERIALIZATION_ID = 1000;

    private Project project;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String throwableClass;
    private String throwable;

    private Collection<ProjectMetrics> projects = new ArrayList<>();
    private Collection<ArtifactMetrics> artifacts = new ArrayList<>();
    private Collection<DependencyMetrics> dependencies = new ArrayList<>();
    private Collection<PluginMetrics> plugins = new ArrayList<>();

    private String log;

    protected SessionMetrics() {
    }

    public SessionMetrics(MavenProject project) {
        this.project = new Project(project);
        setName(project.getName());
        setDescription(project.getDescription());
    }

    public Project getProject() {
        return project;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public SessionMetrics setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public SessionMetrics setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public String getThrowableClass() {
        return throwableClass;
    }

    public String getThrowable() {
        return throwable;
    }

    public SessionMetrics setThrowable(Throwable throwable) {
        if (throwable != null) {
            this.throwable = ClassUtils.getName(throwable);
            this.throwable = ExceptionUtils.getStackTrace(throwable);
        }
        return this;
    }

    public Collection<ProjectMetrics> getProjects() {
        return projects;
    }

    public void setProjects(Collection<ProjectMetrics> projects) {
        requireNonNull(projects);
        this.projects = new ArrayList<>(projects);
    }

    public void addProject(ProjectMetrics project) {
        requireNonNull(project);
        this.projects.add(project);
    }

    public Collection<ArtifactMetrics> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Collection<ArtifactMetrics> artifacts) {
        requireNonNull(projects);
        this.artifacts = new ArrayList<>(artifacts);
    }

    public Collection<DependencyMetrics> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Collection<DependencyMetrics> dependencies) {
        requireNonNull(projects);
        this.dependencies = new ArrayList<>(dependencies);
    }

    public Collection<PluginMetrics> getPlugins() {
        return plugins;
    }

    public void setPlugins(Collection<PluginMetrics> plugins) {
        requireNonNull(projects);
        this.plugins = new ArrayList<>(plugins);
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        requireNonNull(projects);
        this.log = log;
    }

    public void store(OutputStream outputStream) throws IOException {
        Kryo kryo = createKryo();
        try (Output output = new Output(outputStream)) {
            kryo.writeObject(output, this);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SessionMetrics.class.getSimpleName() + "[", "]")
                .add("project=" + project)
                .add("startTime=" + startTime)
                .add("endTime=" + endTime)
                .add("throwableClass='" + throwableClass + "'")
                .add("throwable='" + throwable + "'")
                .add("projects=" + projects.size())
                .add("artifacts=" + artifacts.size())
                .add("dependencies=" + dependencies.size())
                .add("plugins=" + plugins.size())
                .add("log='" + (log != null ? log.length() : NA_STRING) + "'")
                .toString();
    }

    public static SessionMetrics load(Resource resource) throws IOException {
        requireNonNull(resource);
        return load(resource.getInputStream());
    }

    public static SessionMetrics load(InputStream inputStream) throws IOException {
        requireNonNull(inputStream);
        try (Input input = new Input(inputStream)) {
            Kryo kryo = createKryo();
            return kryo.readObject(input, SessionMetrics.class);
        }
    }

    private static Kryo createKryo() {
        Kryo kryo = new Kryo();
        kryo.register(SessionMetrics.class, SERIALIZATION_ID);

        kryo.register(Dependency.class, SERIALIZATION_ID + 5);
        kryo.register(Project.class, SERIALIZATION_ID + 6);

        kryo.register(ProjectMetrics.class, SERIALIZATION_ID + 10);
        kryo.register(ArtifactMetrics.class, SERIALIZATION_ID + 11);
        kryo.register(DependencyMetrics.class, SERIALIZATION_ID + 12);
        kryo.register(MojoMetrics.class, SERIALIZATION_ID + 13);
        kryo.register(PluginMetrics.class, SERIALIZATION_ID + 14);

        kryo.addDefaultSerializer(AtomicInteger.class, new DefaultSerializers.AtomicIntegerSerializer());
        kryo.addDefaultSerializer(AtomicLong.class, new DefaultSerializers.AtomicLongSerializer());

        kryo.register(Duration.class, SERIALIZATION_ID + 100);
        kryo.register(ZonedDateTime.class, SERIALIZATION_ID + 101);

        kryo.register(ArrayList.class, SERIALIZATION_ID + 110);
        kryo.register(HashSet.class, SERIALIZATION_ID + 111);
        kryo.register(HashMap.class, SERIALIZATION_ID + 112);

        kryo.register(AtomicInteger.class, SERIALIZATION_ID + 120);
        kryo.register(AtomicLong.class, SERIALIZATION_ID + 121);

        return kryo;
    }


}
