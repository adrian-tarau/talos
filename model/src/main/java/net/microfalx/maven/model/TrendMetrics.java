package net.microfalx.maven.model;

import net.microfalx.jvm.ServerMetrics;
import net.microfalx.jvm.VirtualMachineMetrics;
import net.microfalx.jvm.model.Process;
import net.microfalx.jvm.model.*;
import net.microfalx.metrics.SeriesStore;
import net.microfalx.resource.Resource;
import org.apache.maven.execution.MavenSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableCollection;

public class TrendMetrics extends AbstractSessionMetrics {

    private Collection<TestSummaryMetrics> tests;
    private Collection<ArtifactSummaryMetrics> artifacts;

    public static TrendMetrics load(Resource resource) throws IOException {
        return AbstractSessionMetrics.load(resource, TrendMetrics.class);
    }

    public static TrendMetrics load(InputStream inputStream) throws IOException {
        return AbstractSessionMetrics.load(inputStream, TrendMetrics.class);
    }

    protected TrendMetrics() {
    }

    public TrendMetrics(MavenSession session) {
        super(session);
    }

    public Collection<TestSummaryMetrics> getTests() {
        return unmodifiableCollection(tests);
    }

    public Collection<ArtifactSummaryMetrics> getArtifacts() {
        return unmodifiableCollection(artifacts);
    }

    public static TrendMetrics from(SessionMetrics sessionMetrics) {
        TrendMetrics trendMetrics = new TrendMetrics();
        copy(sessionMetrics, trendMetrics);
        trendMetrics.setVirtualMachine(getVirtualMachine(sessionMetrics));
        trendMetrics.setServer(getServer(sessionMetrics));
        trendMetrics.tests = TestSummaryMetrics.from(sessionMetrics.getTests());
        trendMetrics.artifacts = ArtifactSummaryMetrics.from(sessionMetrics.getArtifacts());
        return trendMetrics;
    }

    private static VirtualMachine getVirtualMachine(SessionMetrics sessionMetrics) {
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setMemoryPools(new ArrayList<>());
        virtualMachine.setBufferPools(new ArrayList<>());

        Process process = new Process();
        virtualMachine.setProcess(process);
        ThreadInformation threadInformation = new ThreadInformation();
        virtualMachine.setThreadInformation(threadInformation);

        if (sessionMetrics.getVirtualMachineMetrics() != null) {
            updateProcessCpu(process, sessionMetrics.getVirtualMachineMetrics());
            updateProcessMemory(virtualMachine, sessionMetrics.getVirtualMachineMetrics());
            updateProcessGc(virtualMachine, sessionMetrics.getVirtualMachineMetrics());
            updateProcessMisc(threadInformation, sessionMetrics.getVirtualMachineMetrics());
        }

        return virtualMachine;
    }

    private static void updateProcessCpu(Process process, SeriesStore store) {
        process.setCpuTotal((float) store.getAverage(VirtualMachineMetrics.CPU_TOTAL).orElse(0));
        process.setCpuSystem((float) store.getAverage(VirtualMachineMetrics.CPU_SYSTEM).orElse(0));
        process.setCpuUser((float) store.getAverage(VirtualMachineMetrics.CPU_USER).orElse(0));
        process.setCpuIoWait((float) store.getAverage(VirtualMachineMetrics.CPU_IO_WAIT).orElse(0));
        process.setThreads((int) store.getAverage(VirtualMachineMetrics.THREAD).orElse(0));
    }

    private static void updateProcessMemory(VirtualMachine virtualMachine, SeriesStore store) {
        virtualMachine.setHeapTotalMemory((long) store.getAverage(VirtualMachineMetrics.MEMORY_HEAP_MAX).orElse(0));
        virtualMachine.setHeapUsedMemory((long) store.getAverage(VirtualMachineMetrics.MEMORY_HEAP_USED).orElse(0));
        virtualMachine.setNonHeapTotalMemory((long) store.getAverage(VirtualMachineMetrics.MEMORY_NON_HEAP_MAX).orElse(0));
        virtualMachine.setNonHeapUsedMemory((long) store.getAverage(VirtualMachineMetrics.MEMORY_NON_HEAP_USED).orElse(0));
        virtualMachine.setMemoryPools(asList(
                createMemoryPool(MemoryPool.Type.EDEN, store.getAverage(VirtualMachineMetrics.MEMORY_EDEN_MAX).orElse(0),
                        store.getAverage(VirtualMachineMetrics.MEMORY_EDEN_USED).orElse(0)),
                createMemoryPool(MemoryPool.Type.TENURED, store.getAverage(VirtualMachineMetrics.MEMORY_TENURED_MAX).orElse(0),
                        store.getAverage(VirtualMachineMetrics.MEMORY_TENURED_USED).orElse(0))
        ));
    }

    private static void updateProcessGc(VirtualMachine virtualMachine, SeriesStore store) {
        virtualMachine.setGarbageCollections(asList(
                createGarbageCollection(GarbageCollection.Type.EDEN, store.getAverage(VirtualMachineMetrics.GC_EDEN_DURATION).orElse(0),
                        store.getAverage(VirtualMachineMetrics.GC_EDEN_COUNT).orElse(0)),
                createGarbageCollection(GarbageCollection.Type.TENURED, store.getAverage(VirtualMachineMetrics.GC_TENURED_DURATION).orElse(0),
                        store.getAverage(VirtualMachineMetrics.GC_TENURED_COUNT).orElse(0))
        ));
    }

    private static void updateProcessMisc(ThreadInformation threadInformation, SeriesStore store) {
        threadInformation.setDaemon((int) store.getAverage(VirtualMachineMetrics.THREAD_DAEMON).orElse(0));
        threadInformation.setNonDaemon((int) store.getAverage(VirtualMachineMetrics.THREAD_NON_DAEMON).orElse(0));
    }

    private static Server getServer(SessionMetrics sessionMetrics) {
        Server server = new Server();
        if (sessionMetrics.getServer() != null) {
            server.setOs(sessionMetrics.getServer().getOs());
        }
        if (sessionMetrics.getServerMetrics() != null) {
            updateServerCpu(server, sessionMetrics.getServerMetrics());
            updateServerMemory(server, sessionMetrics.getServerMetrics());
            updateServerIo(server, sessionMetrics.getServerMetrics());
            updateServerMisc(server, sessionMetrics.getServerMetrics());
        }
        return server;
    }

    private static void updateServerCpu(Server server, SeriesStore store) {
        server.setCpuTotal((float) store.getAverage(ServerMetrics.CPU_TOTAL).orElse(0));
        server.setCpuSystem((float) store.getAverage(ServerMetrics.CPU_SYSTEM).orElse(0));
        server.setCpuUser((float) store.getAverage(ServerMetrics.CPU_USER).orElse(0));
        server.setCpuNice((float) store.getAverage(ServerMetrics.CPU_NICE).orElse(0));
        server.setCpuIoWait((float) store.getAverage(ServerMetrics.CPU_IO_WAIT).orElse(0));
    }

    private static void updateServerMemory(Server server, SeriesStore store) {
        server.setMemoryTotal((long) store.getAverage(ServerMetrics.MEMORY_MAX).orElse(0));
        server.setMemoryUsed((long) store.getAverage(ServerMetrics.MEMORY_USED).orElse(0));
        server.setMemoryActuallyUsed((long) store.getAverage(ServerMetrics.MEMORY_ACTUALLY_USED).orElse(0));
    }

    private static void updateServerIo(Server server, SeriesStore store) {
        server.setIoReads((long) store.getAverage(ServerMetrics.IO_READS).orElse(0));
        server.setIoReadBytes((long) store.getAverage(ServerMetrics.IO_READ_BYTES).orElse(0));
        server.setIoWrites((long) store.getAverage(ServerMetrics.IO_WRITES).orElse(0));
        server.setIoWriteBytes((long) store.getAverage(ServerMetrics.IO_WRITE_BYTES).orElse(0));
    }

    private static void updateServerMisc(Server server, SeriesStore store) {
        server.setLoad1((float) store.getAverage(ServerMetrics.LOAD_1).orElse(0));
        server.setLoad5((float) store.getAverage(ServerMetrics.LOAD_5).orElse(0));
        server.setLoad15((float) store.getAverage(ServerMetrics.LOAD_15).orElse(0));

        server.setInterrupts((long) store.getAverage(ServerMetrics.INTERRUPTS).orElse(0));
        server.setContextSwitches((long) store.getAverage(ServerMetrics.CONTEXT_SWITCHES).orElse(0));
    }

    private static MemoryPool createMemoryPool(MemoryPool.Type type, double max, double used) {
        return new MemoryPool(type, (long) max, (long) max, (long) used, (long) used);
    }

    private static GarbageCollection createGarbageCollection(GarbageCollection.Type type, double duration, double count) {
        return new GarbageCollection(type, (long) duration, (int) count);
    }


    private static <T> List<T> asList(T... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}
