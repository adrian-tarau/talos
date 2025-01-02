package net.microfalx.boot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationBuilderTest extends AbstractBootstrapTestCase {

    private ApplicationBuilder builder;

    @BeforeEach
    void setup() {
        builder = new ApplicationBuilder();
    }

    @Test
    void getHome() {
        assertEquals("app", builder.getHome().getName());
    }

    @Test
    void getClassPath() {
        assertEquals(3, builder.getClassPath().length);
    }

}