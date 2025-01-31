package net.microfalx.maven.report;

import net.microfalx.lang.JvmUtils;
import net.microfalx.resource.Resource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

class ReportBuilderTest extends AbstractFragmentBuilder {

    @Test
    void singleModule() throws IOException {
        ReportBuilder builder = ReportBuilder.create(createSingleModuleProject());
        Resource resource = Resource.memory();
        builder.build(resource);
        Assertions.assertThat(resource.loadAsString()).contains("html");
    }

    @Test
    void singleModuleOpen() throws IOException {
        ReportBuilder builder = ReportBuilder.create(createSingleModuleProject());
        File file = creaReportFile();
        Resource resource = Resource.file(file);
        builder.build(resource);
        open(resource);
    }

    @Test
    void multiModule() throws IOException {
        ReportBuilder builder = ReportBuilder.create(createMultiModuleProject());
        Resource resource = Resource.memory();
        builder.build(resource);
        Assertions.assertThat(resource.loadAsString()).contains("div");
    }

    @Test
    void multiModuleOpen() throws IOException {
        ReportBuilder builder = ReportBuilder.create(createMultiModuleProject());
        File file = creaReportFile();
        Resource resource = Resource.file(file);
        builder.build(resource);
        open(resource);
    }

    @Test
    void largeMultiModule() throws IOException {
        ReportBuilder builder = ReportBuilder.create(createLargeMultiModuleProject());
        Resource resource = Resource.memory();
        builder.build(resource);
        Assertions.assertThat(resource.loadAsString()).contains("div");
    }

    @Test
    void largeMultiModuleOpen() throws IOException {
        ReportBuilder builder = ReportBuilder.create(createLargeMultiModuleProject());
        File file = creaReportFile();
        Resource resource = Resource.file(file);
        builder.build(resource);
        open(resource);
    }

    private File creaReportFile() {
        return JvmUtils.getTemporaryFile("report_", ".html");
    }

    protected void open(Resource resource) {
        File file = Paths.get(resource.toURI()).toFile();
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            System.out.println("Failed to open " + file.getAbsolutePath());
        }
    }

}