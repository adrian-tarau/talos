package net.microfalx.maven.report;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

class FragmentBuilderTest extends AbstractFragmentBuilder {

    @Test
    void singleModule() throws IOException {
        FragmentBuilder builder = FragmentBuilder.create(FragmentBuilder.Type.SUMMARY, createSingleModuleProject());
        StringWriter writer = new StringWriter();
        builder.build(writer);
        Assertions.assertThat(writer.toString()).contains("aaaa");
    }

    @Test
    void multiModule() throws IOException {
        FragmentBuilder builder = FragmentBuilder.create(FragmentBuilder.Type.SUMMARY, createMultiModuleProject());
        StringWriter writer = new StringWriter();
        builder.build(writer);
        Assertions.assertThat(writer.toString()).contains("aaaa");
    }

}