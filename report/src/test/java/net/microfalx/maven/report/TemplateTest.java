package net.microfalx.maven.report;

import net.microfalx.resource.Resource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateTest {

    @Test
    void invalid() throws IOException {
        Resource resource = Resource.memory();
        Template.create("invalid").render(resource);
        assertEquals("<!DOCTYPE HTML>\n" +
                     "<html>\n" +
                     "<body>\n" +
                     "</html>", resource.loadAsString());
    }

    @Test
    void empty() throws IOException {
        Resource resource = Resource.memory();
        Template.create("empty").render(resource);
        assertEquals("<!DOCTYPE HTML>\n" +
                     "<html>\n" +
                     "<body>\n" +
                     "</body>\n" +
                     "</html>", resource.loadAsString());
    }

    @Test
    void variables() throws IOException {
        Resource resource = Resource.memory();
        Template.create("variables")
                .addVariable("body", "my-body")
                .addVariable("list", List.of("a", "b", "c"))
                .render(resource);
        assertEquals("<!DOCTYPE HTML>\n" +
                     "<html>\n" +
                     "<body css=\"my-body\">\n" +
                     "    <p>a</p>\n" +
                     "    <p>b</p>\n" +
                     "    <p>c</p>\n" +
                     "</body>\n" +
                     "</html>", resource.loadAsString());
    }

    @Test
    void fragments() throws IOException {
        Resource resource = Resource.memory();
        Template.create("fragments").setSelector("f1").addVariable("value",10).render(resource);
        assertEquals("<div>\n" +
                     "        <span>10</span>\n" +
                     "    </div>", resource.loadAsString());
    }

}