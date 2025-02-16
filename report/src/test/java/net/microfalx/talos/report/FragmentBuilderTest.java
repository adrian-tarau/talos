package net.microfalx.talos.report;

import net.microfalx.resource.Resource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class FragmentBuilderTest extends AbstractFragmentBuilder {

    @Test
    void singleModule() throws IOException {
        FragmentBuilder builder = FragmentBuilder.create(Fragment.create(Fragment.Type.SUMMARY), createSingleModuleProject());
        Resource resource = Resource.memory();
        builder.build(resource);
        Assertions.assertThat(resource.loadAsString()).contains("div");
    }

    @Test
    void multiModule() throws IOException {
        FragmentBuilder builder = FragmentBuilder.create(Fragment.create(Fragment.Type.SUMMARY), createMultiModuleProject());
        Resource resource = Resource.memory();
        builder.build(resource);
        Assertions.assertThat(resource.loadAsString()).contains("div");
    }

}