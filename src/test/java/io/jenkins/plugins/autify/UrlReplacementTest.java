package io.jenkins.plugins.autify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.module.ModuleDescriptor.Version;

import org.junit.Test;

import io.jenkins.plugins.autify.model.UrlReplacement;

public class UrlReplacementTest {
    @Test
    public void testSpaceDelimiter() throws Exception {
        UrlReplacement urlReplacement = new UrlReplacement("https://example.com", "https://example.net");
        assertEquals("https://example.com https://example.net", urlReplacement.toCliString(Version.parse("0.29.0")));
    }

    @Test
    public void testEqualDelimiter() throws Exception {
        UrlReplacement urlReplacement = new UrlReplacement("https://example.com", "https://example.net");
        assertEquals("https://example.com=https://example.net", urlReplacement.toCliString(Version.parse("0.28.0")));
    }
}
