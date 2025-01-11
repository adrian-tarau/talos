package net.microfalx.maven.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}