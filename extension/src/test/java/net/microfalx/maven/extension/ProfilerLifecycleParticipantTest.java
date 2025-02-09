package net.microfalx.maven.extension;

import net.microfalx.maven.core.MavenLogger;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.joor.Reflect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfilerLifecycleParticipantTest extends AbstractExtensionTestCase {

    private static final MavenLogger LOGGER = MavenLogger.create(ProfilerLifecycleParticipantTest.class);

    private ProfilerLifecycleParticipant participant;

    @BeforeEach
    void setup() throws MavenExecutionException {
        participant = new ProfilerLifecycleParticipant();
        prepare();
    }

    @Test
    void storeMetrics() throws MavenExecutionException {
        Reflect.on(participant).call("storeMetrics", getSession());
    }

    @Test
    void collectExtensionEvents() {
        Reflect.on(participant).call("collectExtensionEvents");
    }

    private void prepare() throws MavenExecutionException {
        MavenSession session = initSession();
        Reflect.on(participant).set("mavenLogger", getLogger());
        participant.afterSessionStart(session);
        participant.afterProjectsRead(session);
    }


}