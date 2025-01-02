package net.microfalx.boot;

import org.junit.jupiter.api.BeforeEach;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

abstract class AbstractBootstrapTestCase {

    @BeforeEach
    void setupApp() throws URISyntaxException {
        URL app = getClass().getClassLoader().getResource("app");
        if (app == null) throw new IllegalStateException("The application directory could not be located");
        System.setProperty("microfalx.home", Paths.get(app.toURI()).toFile().getAbsolutePath());
    }
}
