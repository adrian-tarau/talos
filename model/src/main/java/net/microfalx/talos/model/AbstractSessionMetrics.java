package net.microfalx.talos.model;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.ImmutableCollectionsSerializers;
import com.esotericsoftware.kryo.serializers.OptionalSerializers;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import net.microfalx.jvm.model.Process;
import net.microfalx.jvm.model.*;
import net.microfalx.lang.IOUtils;
import net.microfalx.metrics.DefaultSeries;
import net.microfalx.metrics.Metric;
import net.microfalx.metrics.SeriesMemoryStore;
import net.microfalx.metrics.Value;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableCollection;
import static net.microfalx.lang.ArgumentUtils.requireNonNull;

public abstract class AbstractSessionMetrics<T extends AbstractSessionMetrics<T>> extends AbstractTimeAwareMetrics<T> {

    private static final int SERIALIZATION_ID = 1000;

    private Project project;

    private final Collection<ProjectMetrics> modules = new ArrayList<>();
    private final Collection<MojoMetrics> mojos = new ArrayList<>();
    private final Collection<LifecycleMetrics> lifecycles = new ArrayList<>();
    private final Collection<FailureMetrics> extensionFailures = new ArrayList<>();
    private final Collection<ExtensionMetrics> extensions = new ArrayList<>();

    private final Collection<String> profiles = new ArrayList<>();
    private final Collection<String> goals = new ArrayList<>();
    private boolean offline = false;
    private boolean verbose;
    private int dop;

    private URI localRepository;
    private final Collection<URI> remoteRepositories = new ArrayList<>();

    private transient Map<String, ProjectMetrics> modulesById;
    private transient Map<String, MojoMetrics> mojosById;
    private transient Map<String, LifecycleMetrics> lifeCyclesById;

    protected AbstractSessionMetrics() {
    }

    public AbstractSessionMetrics(MavenSession session) {
        this.project = new Project(session.getTopLevelProject(), true);
        setId(project.getId());
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

    public boolean isVerbose() {
        return verbose;
    }

    public T setVerbose(boolean verbose) {
        this.verbose = verbose;
        return self();
    }

    public URI getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(URI localRepository) {
        this.localRepository = localRepository;
    }

    public Collection<URI> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(Collection<URI> remoteRepositories) {
        requireNonNull(remoteRepositories);
        this.remoteRepositories.addAll(remoteRepositories);
    }

    public ProjectMetrics getModule(String id) {
        requireNonNull(id);
        if (modulesById == null) {
            modulesById = new HashMap<>();
            for (ProjectMetrics module : modules) {
                modulesById.put(module.getId(), module);
                modulesById.put(module.getArtifactId(), module);
            }
        }
        ProjectMetrics metrics = modulesById.get(id);
        if (metrics == null) throw new IllegalArgumentException("A module with id " + id + " does not exist");
        return metrics;
    }

    public boolean isMultiModule() {
        return modules.size() > 1;
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

    public MojoMetrics getMojo(String id) {
        requireNonNull(id);
        if (mojosById == null) {
            mojosById = new HashMap<>();
            for (MojoMetrics mojo : mojos) {
                mojosById.put(mojo.getId(), mojo);
            }
        }
        MojoMetrics metrics = mojosById.get(id);
        if (metrics == null) throw new IllegalArgumentException("A Mojo with id " + id + " does not exist");
        return metrics;
    }

    public void setMojos(Collection<MojoMetrics> mojos) {
        requireNonNull(modules);
        this.mojos.addAll(mojos);
    }

    public Collection<LifecycleMetrics> getLifecycles() {
        return lifecycles;
    }

    public LifecycleMetrics getLifecycle(String id) {
        requireNonNull(id);
        if (lifeCyclesById == null) {
            lifeCyclesById = new HashMap<>();
            for (LifecycleMetrics lifecycle : lifecycles) {
                lifeCyclesById.put(lifecycle.getId(), lifecycle);
            }
        }
        LifecycleMetrics metrics = lifeCyclesById.get(id);
        if (metrics == null) throw new IllegalArgumentException("A life cycle with id " + id + " does not exist");
        return metrics;
    }

    public void setLifeCycles(Collection<LifecycleMetrics> lifecycles) {
        requireNonNull(lifecycles);
        this.lifecycles.addAll(lifecycles);
    }

    public Collection<ExtensionMetrics> getExtensions() {
        return unmodifiableCollection(extensions);
    }

    public void setExtensions(Collection<ExtensionMetrics> extensions) {
        requireNonNull(extensions);
        this.extensions.addAll(extensions);
    }

    public Collection<FailureMetrics> getProjectFailures() {
        Collection<FailureMetrics> projectFailures = new ArrayList<>();
        projectFailures.addAll(modules.stream().map(ProjectMetrics::getFailureMetrics).filter(Objects::nonNull).collect(Collectors.toList()));
        projectFailures.addAll(mojos.stream().map(MojoMetrics::getFailureMetrics).filter(Objects::nonNull).collect(Collectors.toList()));
        projectFailures.forEach(this::updateFailureMetrics);
        return unmodifiableCollection(projectFailures);
    }

    public Collection<FailureMetrics> getExtensionFailures() {
        extensionFailures.forEach(this::updateFailureMetrics);
        return unmodifiableCollection(extensionFailures);
    }

    public void setExtensionFailures(Collection<FailureMetrics> failures) {
        requireNonNull(failures);
        this.extensionFailures.addAll(failures);
    }

    public void addExtensionFailure(FailureMetrics failure) {
        requireNonNull(failure);
        this.extensionFailures.add(failure);
    }

    public void store(OutputStream outputStream) throws IOException {
        Kryo kryo = createKryo();
        outputStream = IOUtils.getCompressedOutputStream(outputStream);
        try (Output output = new Output(outputStream)) {
            kryo.writeObject(output, this);
        }
    }

    private void updateFailureMetrics(FailureMetrics failure) {
        if (failure.getModuleId() != null && failure.getModule() == null) {
            failure.module = getModule(failure.getModuleId());
        }
        if (failure.getMojoId() != null && failure.getMojo() == null) {
            failure.mojo = getMojo(failure.getMojoId());
        }
    }

    public static <T extends AbstractSessionMetrics> T load(Resource resource, Class<T> type) throws IOException {
        requireNonNull(resource);
        return load(resource.getInputStream(), type);
    }

    public static <T extends AbstractSessionMetrics> T load(InputStream inputStream, Class<T> type) throws IOException {
        requireNonNull(inputStream);
        inputStream = IOUtils.getComporessedInputStream(inputStream);
        try (Input input = new Input(inputStream)) {
            Kryo kryo = createKryo();
            return kryo.readObject(input, type);
        }
    }

    protected static void copy(AbstractSessionMetrics<?> source, AbstractSessionMetrics<?> target) {
        requireNonNull(source);
        requireNonNull(target);
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.project = source.getProject();
        target.updateInterval(source.getStartTime(), source.getEndTime());
        target.localRepository = source.localRepository;
        target.remoteRepositories.addAll(source.remoteRepositories);

        target.modules.addAll(source.modules);
        target.mojos.addAll(source.mojos);
        target.lifecycles.addAll(source.lifecycles);
        target.extensionFailures.addAll(source.extensionFailures);
        target.extensions.addAll(source.extensions);

        target.profiles.addAll(source.profiles);
        target.goals.addAll(source.goals);
        target.offline = source.offline;
        target.verbose = source.verbose;
        target.dop = source.dop;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("project=" + project)
                .add("startTime=" + getStartTime())
                .add("endTime=" + getEndTime())
                .add("modules=" + modules)
                .add("mojos=" + mojos)
                .add("lifecycles=" + lifecycles.size())
                .add("extensionFailures=" + extensionFailures.size())
                .add("profiles=" + profiles.size())
                .add("goals=" + goals.size())
                .add("offline=" + offline)
                .add("verbose=" + verbose)
                .add("dop=" + dop)
                .toString();
    }

    protected static Kryo createKryo() {
        Kryo kryo = new Kryo();
        kryo.setDefaultSerializer(VersionFieldSerializer.class);
        kryo.register(SessionMetrics.class, SERIALIZATION_ID);
        kryo.register(TrendMetrics.class, SERIALIZATION_ID + 1);

        kryo.register(Dependency.class, SERIALIZATION_ID + 5);
        kryo.register(Project.class, SERIALIZATION_ID + 6);
        kryo.register(TestMetrics.class, SERIALIZATION_ID + 7);

        kryo.register(ProjectMetrics.class, SERIALIZATION_ID + 20);
        kryo.register(ArtifactMetrics.class, SERIALIZATION_ID + 21);
        kryo.register(ArtifactSummaryMetrics.class, SERIALIZATION_ID + 22);
        kryo.register(DependencyMetrics.class, SERIALIZATION_ID + 23);
        kryo.register(MojoMetrics.class, SERIALIZATION_ID + 24);
        kryo.register(PluginMetrics.class, SERIALIZATION_ID + 25);
        kryo.register(ExtensionMetrics.class, SERIALIZATION_ID + 26);
        kryo.register(TestMetrics.class, SERIALIZATION_ID + 27);
        kryo.register(TestSummaryMetrics.class, SERIALIZATION_ID + 28);
        kryo.register(LifecycleMetrics.class, SERIALIZATION_ID + 29);
        kryo.register(FailureMetrics.class, SERIALIZATION_ID + 30);

        kryo.register(AbstractTimeAwareMetrics.ActiveIntervalImpl.class, SERIALIZATION_ID + 40);

        kryo.register(Metric.class, SERIALIZATION_ID + 50);
        kryo.register(Metric.Type.class, SERIALIZATION_ID + 51);
        kryo.register(Value.class, SERIALIZATION_ID + 52);
        kryo.register(SeriesMemoryStore.class, SERIALIZATION_ID + 53);
        kryo.register(DefaultSeries.class, SERIALIZATION_ID + 54);

        kryo.register(Process.class, SERIALIZATION_ID + 60);
        kryo.register(VirtualMachine.class, SERIALIZATION_ID + 61);
        kryo.register(MemoryPool.class, SERIALIZATION_ID + 62);
        kryo.register(MemoryPool.Type.class, SERIALIZATION_ID + 63);
        kryo.register(BufferPool.class, SERIALIZATION_ID + 64);
        kryo.register(BufferPool.Type.class, SERIALIZATION_ID + 65);
        kryo.register(GarbageCollection.class, SERIALIZATION_ID + 66);
        kryo.register(GarbageCollection.Type.class, SERIALIZATION_ID + 67);
        kryo.register(RuntimeInformation.class, SERIALIZATION_ID + 68);
        kryo.register(ThreadInformation.class, SERIALIZATION_ID + 69);
        kryo.register(Thread.State.class, SERIALIZATION_ID + 70);
        kryo.register(ThreadDump.class, SERIALIZATION_ID + 71);
        kryo.register(ThreadInformation.class, SERIALIZATION_ID + 72);

        kryo.register(Server.class, SERIALIZATION_ID + 80);
        kryo.register(Os.class, SERIALIZATION_ID + 81);
        kryo.register(FileSystem.class, SERIALIZATION_ID + 82);
        kryo.register(FileSystem.Type.class, SERIALIZATION_ID + 83);

        kryo.addDefaultSerializer(AtomicInteger.class, new DefaultSerializers.AtomicIntegerSerializer());
        kryo.addDefaultSerializer(AtomicLong.class, new DefaultSerializers.AtomicLongSerializer());
        kryo.addDefaultSerializer(URI.class, new DefaultSerializers.URISerializer());
        kryo.addDefaultSerializer(Optional.class, new OptionalSerializers.OptionalSerializer());
        kryo.addDefaultSerializer(OptionalDouble.class, new OptionalSerializers.OptionalDoubleSerializer());
        kryo.addDefaultSerializer(ConcurrentSkipListMap.class, new DefaultSerializers.ConcurrentSkipListMapSerializer());
        ImmutableCollectionsSerializers.addDefaultSerializers(kryo);
        ImmutableCollectionsSerializers.addDefaultSerializers(kryo);

        kryo.register(Duration.class, SERIALIZATION_ID + 100);
        kryo.register(ZonedDateTime.class, SERIALIZATION_ID + 101);
        kryo.register(URI.class, SERIALIZATION_ID + 102);
        kryo.register(Optional.class, SERIALIZATION_ID + 103);
        kryo.register(OptionalDouble.class, SERIALIZATION_ID + 104);

        kryo.register(ArrayList.class, SERIALIZATION_ID + 110);
        kryo.register(HashSet.class, SERIALIZATION_ID + 111);
        kryo.register(HashMap.class, SERIALIZATION_ID + 112);
        kryo.register(ConcurrentSkipListMap.class, SERIALIZATION_ID + 113);
        kryo.register(CopyOnWriteArrayList.class, SERIALIZATION_ID + 114);
        kryo.register(CopyOnWriteArraySet.class, SERIALIZATION_ID + 115);

        kryo.register(AtomicInteger.class, SERIALIZATION_ID + 120);
        kryo.register(AtomicLong.class, SERIALIZATION_ID + 121);

        return kryo;
    }
}
