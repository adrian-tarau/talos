package net.microfalx.maven.docker;

import net.microfalx.lang.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Image image = builder.build();
        assertNotNull(image);
    }

}