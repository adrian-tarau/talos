package net.microfalx.boot;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BootstrapTest extends AbstractBootstrapTestCase {

    private Bootstrap bootstrap;

    @BeforeEach
    void setup() {
        bootstrap = new Bootstrap();
        bootstrap.setExitCode(-1);
    }

    @Test
    void noArguments() {
        bootstrap.start(new String[]{"net.microfalx.boot.BootstrapTest$ApplicationMain"});
        Assertions.assertThat(bootstrap.getLog()).contains("Starting application")
                .contains("started successfully");
    }

    @Test
    void noMainMethod() {
        bootstrap.start(new String[]{"org.apache.maven.settings.Repository"});
        Assertions.assertThat(bootstrap.getLog()).contains("Starting application")
                .contains("does not have a main method");
    }

    public static class ApplicationMain {

        public static void main(String[] args) {
            System.out.println("Starting Application");
        }
    }

}