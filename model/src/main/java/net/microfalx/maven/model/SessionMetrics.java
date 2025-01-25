package net.microfalx.maven.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.OptionalSerializers;
import net.microfalx.lang.ClassUtils;
import net.microfalx.lang.ExceptionUtils;
import net.microfalx.lang.NamedIdentityAware;
import net.microfalx.metrics.*;
import net.microfalx.resource.Resource;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.unmodifiableCollection;
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

    private final Collection<ProjectMetrics> modules = new ArrayList<>();
    private final Collection<ArtifactMetrics> artifacts = new ArrayList<>();
    private final Collection<DependencyMetrics> dependencies = new ArrayList<>();
    private final Collection<MojoMetrics> mojos = new ArrayList<>();
    private final Collection<PluginMetrics> plugins = new ArrayList<>();
    private final Collection<TestMetrics> tests = new ArrayList<>();

    private final Collection<String> profiles = new ArrayList<>();
    private final Collection<String> goals = new ArrayList<>();
    private boolean offline = false;
    private int dop;

    private SeriesStore jvm = SeriesStore.memory();
    private SeriesStore server = SeriesStore.memory();

    private String log;

    protected SessionMetrics() {
    }

    public SessionMetrics(MavenSession session) {
        requireNonNull(session);
        this.project = new Project(session.getTopLevelProject());
        setName(project.getName());
        setDescription(project.getDescription());
        MavenExecutionRequest request = session.getRequest();
        profiles.addAll(request.getActiveProfiles());
        goals.addAll(request.getGoals());
        offline = request.isOffline();
        dop = request.getDegreeOfConcurrency();
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

    public Collection<String> getProfiles() {
        return profiles;
    }

    public Collection<String> getGoals() {
        return goals;
    }

    public boolean isOffline() {
        return offline;
    }

    public int getDop() {
        return dop;
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

    public Collection<ProjectMetrics> getModules() {
        return unmodifiableCollection(modules);
    }

    public void setModules(Collection<ProjectMetrics> modules) {
        requireNonNull(modules);
        this.modules.addAll(modules);
    }

    public void addModule(ProjectMetrics module) {
        requireNonNull(module);
        this.modules.add(module);
    }

    public Collection<MojoMetrics> getMojos() {
        return unmodifiableCollection(mojos);
    }

    public void setMojos(Collection<MojoMetrics> mojos) {
        requireNonNull(modules);
        this.mojos.addAll(mojos);
    }

    public Collection<ArtifactMetrics> getArtifacts() {
        return unmodifiableCollection(artifacts);
    }

    public void setArtifacts(Collection<ArtifactMetrics> artifacts) {
        requireNonNull(modules);
        this.artifacts.addAll(artifacts);
    }

    public Collection<DependencyMetrics> getDependencies() {
        return unmodifiableCollection(dependencies);
    }

    public void setDependencies(Collection<DependencyMetrics> dependencies) {
        requireNonNull(modules);
        this.dependencies.addAll(dependencies);
    }

    public Collection<PluginMetrics> getPlugins() {
        return unmodifiableCollection(plugins);
    }

    public void setPlugins(Collection<PluginMetrics> plugins) {
        requireNonNull(modules);
        this.plugins.addAll(plugins);
    }

    public Collection<TestMetrics> getTests() {
        return unmodifiableCollection(tests);
    }

    public void setTests(Collection<TestMetrics> tests) {
        requireNonNull(tests);
        this.tests.addAll(tests);
    }

    public SeriesStore getJvm() {
        return jvm;
    }

    public void setJvm(SeriesStore jvm) {
        this.jvm = jvm;
    }

    public SeriesStore getServer() {
        return server;
    }

    public void setServer(SeriesStore server) {
        this.server = server;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        requireNonNull(modules);
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
                .add("projects=" + modules.size())
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
        kryo.register(TestMetrics.class, SERIALIZATION_ID + 7);

        kryo.register(ProjectMetrics.class, SERIALIZATION_ID + 20);
        kryo.register(ArtifactMetrics.class, SERIALIZATION_ID + 21);
        kryo.register(DependencyMetrics.class, SERIALIZATION_ID + 22);
        kryo.register(MojoMetrics.class, SERIALIZATION_ID + 23);
        kryo.register(PluginMetrics.class, SERIALIZATION_ID + 24);
        kryo.register(TestMetrics.class, SERIALIZATION_ID + 25);

        kryo.register(Metric.class, SERIALIZATION_ID + 30);
        kryo.register(Value.class, SERIALIZATION_ID + 31);
        kryo.register(SeriesMemoryStore.class, SERIALIZATION_ID + 32);
        kryo.register(DefaultSeries.class, SERIALIZATION_ID + 33);

        kryo.addDefaultSerializer(AtomicInteger.class, new DefaultSerializers.AtomicIntegerSerializer());
        kryo.addDefaultSerializer(AtomicLong.class, new DefaultSerializers.AtomicLongSerializer());
        kryo.addDefaultSerializer(URI.class, new DefaultSerializers.URISerializer());
        kryo.addDefaultSerializer(Optional.class, new OptionalSerializers.OptionalSerializer());
        kryo.addDefaultSerializer(OptionalDouble.class, new OptionalSerializers.OptionalDoubleSerializer());
        kryo.addDefaultSerializer(ConcurrentSkipListMap.class, new DefaultSerializers.ConcurrentSkipListMapSerializer());

        kryo.register(Duration.class, SERIALIZATION_ID + 100);
        kryo.register(ZonedDateTime.class, SERIALIZATION_ID + 101);
        kryo.register(URI.class, SERIALIZATION_ID + 102);
        kryo.register(Optional.class, SERIALIZATION_ID + 103);
        kryo.register(OptionalDouble.class, SERIALIZATION_ID + 104);

        kryo.register(ArrayList.class, SERIALIZATION_ID + 110);
        kryo.register(HashSet.class, SERIALIZATION_ID + 111);
        kryo.register(HashMap.class, SERIALIZATION_ID + 112);
        kryo.register(ConcurrentSkipListMap.class, SERIALIZATION_ID + 113);

        kryo.register(AtomicInteger.class, SERIALIZATION_ID + 120);
        kryo.register(AtomicLong.class, SERIALIZATION_ID + 121);

        return kryo;
    }


}
