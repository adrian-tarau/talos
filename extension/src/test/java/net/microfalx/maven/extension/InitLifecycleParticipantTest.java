package net.microfalx.maven.extension;

import net.microfalx.maven.core.MavenLogger;
import org.junit.jupiter.api.Test;

class InitLifecycleParticipantTest {

    private static final MavenLogger LOGGER = MavenLogger.create(InitLifecycleParticipantTest.class);

    @Test
    void initLogging() {
        InitLifecycleParticipant participant = new InitLifecycleParticipant();
        participant.initLogging();

    }

}