package net.microfalx.maven.core;

import org.junit.jupiter.api.Test;

class MavenLoggerTest {

    @Test
    void initLogging() {
        MavenLogger logger = new MavenLogger();
        logger.initLogging();

    }

}