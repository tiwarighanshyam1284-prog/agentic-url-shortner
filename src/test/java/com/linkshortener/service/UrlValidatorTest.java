package com.linkshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UrlValidatorTest {

    private final UrlValidator validator = new UrlValidator();

    @Test
    void acceptsPublicHttpAndHttpsAddresses() {
        assertEquals("https://example.com/docs", validator.requireTargetUrl("  https://example.com/docs  "));
        assertEquals("http://www.oracle.com/", validator.requireTargetUrl("http://www.oracle.com/"));
    }

    @Test
    void acceptsHostNamesThatOnlyStartLikeInternalOnes() {
        // "fdic" and "fcc" begin with the letters of a private IPv6 prefix but are ordinary names
        validator.requireTargetUrl("https://fdic.gov/");
        validator.requireTargetUrl("https://fcc.gov/");
        validator.requireTargetUrl("https://www.fedex.com/");
    }

    @Test
    void rejectsOtherSchemes() {
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("ftp://example.com/file"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("javascript:alert(1)"));
    }

    @Test
    void rejectsLocalAndPrivateHosts() {
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://localhost:9000/x"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://127.0.0.1/"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://10.1.2.3/"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://192.168.0.5/"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://172.20.0.1/"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://[::1]/"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://[fd00::1]/"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://[fe80::1]/"));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("http://intranet/"));
    }

    @Test
    void rejectsMissingOrCredentialedAddresses() {
        assertThrows(ApiException.class, () -> validator.requireTargetUrl(null));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("   "));
        assertThrows(ApiException.class, () -> validator.requireTargetUrl("https://user:secret@example.com/"));
    }

    @Test
    void normalisesAliasesToLowerCase() {
        assertEquals("my-docs", validator.normaliseAlias("My-Docs"));
    }

    @Test
    void rejectsBadAndReservedAliases() {
        assertThrows(ApiException.class, () -> validator.normaliseAlias("ab"));
        assertThrows(ApiException.class, () -> validator.normaliseAlias("has_underscore"));
        assertThrows(ApiException.class, () -> validator.normaliseAlias("-leading"));
        assertThrows(ApiException.class, () -> validator.normaliseAlias("api"));
    }
}
