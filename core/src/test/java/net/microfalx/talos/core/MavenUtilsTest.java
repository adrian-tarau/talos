package net.microfalx.talos.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MavenUtilsTest {

    @Test
    void formatInteger() {
        assertEquals("    1", MavenUtils.formatInteger(1, 5));
        assertEquals("   12", MavenUtils.formatInteger(12, 5));
        assertEquals("  123", MavenUtils.formatInteger(123, 5));
        assertEquals(" 1234", MavenUtils.formatInteger(1234, 5));
        assertEquals("12345", MavenUtils.formatInteger(12345, 5));
        assertEquals("123456", MavenUtils.formatInteger(123456, 5));
    }

    @Test
    void leftPad() {
        assertEquals("     ", MavenUtils.leftPad(null, 5));
        assertEquals("    a", MavenUtils.leftPad("a", 5));
        assertEquals("   ab", MavenUtils.leftPad("ab", 5));
        assertEquals("  abc", MavenUtils.leftPad("abc", 5));
        assertEquals(" abcd", MavenUtils.leftPad("abcd", 5));
        assertEquals("abcde", MavenUtils.leftPad("abcde", 5));
        assertEquals("abcdef", MavenUtils.leftPad("abcdef", 5));
    }

    @Test
    void maskSecret() {
        assertEquals("*********************", MavenUtils.maskSecret("password", "p1"));
        assertEquals("http://secret.domain/path?q1=1#a", MavenUtils.maskSecret("url", "http://maven.apache.org/path?q1=1#a"));
    }

    @Test
    void isVerboseGoal() {
        assertFalse(MavenUtils.isVerboseGoal(Arrays.asList("clean", "install")));
        assertTrue(MavenUtils.isVerboseGoal(Arrays.asList("dependency:tree")));
        assertTrue(MavenUtils.isVerboseGoal(Arrays.asList("dependency:aaa")));
    }

}