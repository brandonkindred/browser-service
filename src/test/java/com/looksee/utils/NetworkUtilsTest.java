package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

public class NetworkUtilsTest {

    @Test
    public void testReadUrlWithInvalidHost() {
        // Should throw IOException for unreachable host
        assertThrows(IOException.class, () ->
                NetworkUtils.readUrl(new URL("https://this-host-does-not-exist-12345.example.com/style.css")));
    }

    @Test
    public void testReadUrlWithHttpThrowsClassCast() {
        // HTTP URLs will fail because readUrl casts to HttpsURLConnection
        assertThrows(ClassCastException.class, () ->
                NetworkUtils.readUrl(new URL("http://example.com/style.css")));
    }
}
