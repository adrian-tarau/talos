package net.microfalx.maven.docker;

import net.microfalx.lang.FileUtils;
import net.microfalx.lang.StringUtils;
import net.microfalx.resource.Resource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ImageBuilderTest {

    private ImageBuilder builder;

    @BeforeEach
    void setup() {
        builder = new ImageBuilder("microfalx-base")
                .setBase(true);
    }

    @Test
    void defaults() {
        ImageBuilder builder = new ImageBuilder("microfalx-base");
        assertNull(builder.getWorkspaceDirectory());
        assertEquals("microfalx", builder.getUser());
        assertEquals("/opt/microfalx", FileUtils.toUnix(builder.getPath()));
    }

    @Test
    void base() {
        Assertions.assertThat(builder.buildDescriptor()).contains("/opt/microfalx")
                .contains("ENTRYPOINT").contains("HEALTHCHECK").contains("apt-get")
                .contains("HOME=").contains("PATH=")
                .doesNotContain("APP_MAIN_CLASS");
    }

    @Test
    void app() {
        builder.setBase(false).setImage("base.latest")
                .setMainClass("net.microfalx.maven.Test");
        Assertions.assertThat(builder.buildDescriptor()).doesNotContain("apt-get")
                .contains("base.latest").contains("COPY")
                .contains("APP_MAIN_CLASS").contains("net.microfalx.maven.Test");
    }

    @Test
    void build() {
        buildAndTag(null);
    }

    @Test
    void buildAndTag() {
        buildAndTag("adriantarau");
    }

    void buildAndTag(String repository) {
        builder.setMainClass("net.microfalx.maven.Test");
        if (repository != null) {
            builder.setRepository(repository);
        }
        //addLibraries();
        Image image = builder.build();
        assertNotNull(image);
        assertEquals("microfalx-base", image.getName());
        assertEquals(1, image.getTags().size());
        assertEquals("amd64", image.getArchitecture());
        assertEquals("linux", image.getOs());
        assertTrue(image.getDigest().startsWith("sha256"));
    }

    private void addLibraries() {
        String libraries = System.getProperty("java.class.path");
        Arrays.stream(StringUtils.split(libraries, File.pathSeparator))
                .map(File::new).filter(file -> !file.getName().contains("idea")).filter(File::isFile)
                .forEach(file -> builder.addLibrary(Resource.file(file)));
    }

}