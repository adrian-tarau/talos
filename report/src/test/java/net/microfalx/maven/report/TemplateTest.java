package net.microfalx.maven.report;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateTest {

    @Test
    void invalid() throws IOException {
        StringWriter writer = new StringWriter();
        Template.create("invalid").render(writer);
        assertEquals("<!DOCTYPE HTML>\n" +
                     "<html>\n" +
                     "<body>\n" +
                     "</html>", writer.toString());
    }

    @Test
    void empty() throws IOException {
        StringWriter writer = new StringWriter();
        Template.create("empty").render(writer);
        assertEquals("<!DOCTYPE HTML>\n" +
                     "<html>\n" +
                     "<body>\n" +
                     "</body>\n" +
                     "</html>", writer.toString());
    }

    @Test
    void variables() throws IOException {
        StringWriter writer = new StringWriter();
        Template.create("variables")
                .addVariable("body", "my-body")
                .addVariable("list", List.of("a", "b", "c"))
                .render(writer);
        assertEquals("<!DOCTYPE HTML>\n" +
                     "<html>\n" +
                     "<body css=\"my-body\">\n" +
                     "    <p>a</p>\n" +
                     "    <p>b</p>\n" +
                     "    <p>c</p>\n" +
                     "</body>\n" +
                     "</html>", writer.toString());
    }

    @Test
    void fragments() throws IOException {
        StringWriter writer = new StringWriter();
        Template.create("fragments").setSelector("f1").addVariable("value",10).render(writer);
        assertEquals("<div>\n" +
                     "        <span>10</span>\n" +
                     "    </div>", writer.toString());
    }

}