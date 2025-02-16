package net.microfalx.talos.model;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionMetricsTest extends AbstractMetricsTest {

    @Test
    void store() throws IOException {
        SessionMetrics session = create();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        session.store(outputStream);
        assertTrue(outputStream.toByteArray().length > 0);
    }

    @Test
    void load() throws IOException {
        SessionMetrics session = create();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        session.store(outputStream);
        SessionMetrics restoredSession = SessionMetrics.load(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals(session.getName(), restoredSession.getName());
    }



}