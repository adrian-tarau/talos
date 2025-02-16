package net.microfalx.talos.core;

import org.junit.jupiter.api.Test;

class MavenLoggerTest {

    @Test
    void initLogging() {
        MavenLogger logger = new MavenLogger();
        logger.initLogging();

    }

}