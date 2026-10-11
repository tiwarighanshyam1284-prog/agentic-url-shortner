package com.linkshortener.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.linkshortener.model.ShortLink;
import com.linkshortener.repository.InMemoryLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkServiceTest {

    private MutableClock clock;
    private LinkService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new LinkService(new InMemoryLinkRepository(), new CodeGenerator(), new UrlValidator(), clock);
    }

    @Test
    void createsRandomSevenCharacterCode() {
        ShortLink link = service.create("https://example.com/a", null, null);
        assertEquals(7, link.code().length());
        assertEquals(false, link.customAlias());
        assertEquals("https://example.com/a", link.targetUrl());
    }

    @Test
    void usesAliasAsCodeAndRejectsTheSecondUse() {
        ShortLink link = service.create("https://example.com/", "docs", null);
        assertEquals("docs", link.code());
        assertTrue(link.customAlias());
        ApiException ex = assertThrows(ApiException.class,
                () -> service.create("https://example.org/", "docs", null));
        assertEquals(409, ex.status().value());
    }

    @Test
    void followingCountsClicksAndRecordsTheTime() {
        service.create("https://example.com/", "counted", null);
        service.followForRedirect("counted");
        clock.advanceSeconds(5);
        ShortLink link = service.followForRedirect("counted");
        assertEquals(2, link.totalClicks());
        assertEquals(clock.instant(), link.lastClickedAt());
    }

    @Test
    void expiredLinkIsGoneAndNotCounted() {
        service.create("https://example.com/", "short", 60L);
        service.followForRedirect("short");
        clock.advanceSeconds(60);
        ApiException ex = assertThrows(ApiException.class, () -> service.followForRedirect("short"));
        assertEquals(410, ex.status().value());
        assertEquals(1, service.find("short").totalClicks());
    }

    @Test
    void disabledLinkIsGoneAndDisablingTwiceIsHarmless() {
        service.create("https://example.com/", "off", null);
        service.disable("off");
        service.disable("off");
        ApiException ex = assertThrows(ApiException.class, () -> service.followForRedirect("off"));
        assertEquals(410, ex.status().value());
    }

    @Test
    void unknownCodeIsNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.find("nope123"));
        assertEquals(404, ex.status().value());
    }

    @Test
    void rejectsTtlOutsideOneYear() {
        assertThrows(ApiException.class, () -> service.create("https://example.com/", null, 0L));
        assertThrows(ApiException.class,
                () -> service.create("https://example.com/", null, LinkService.MAX_TTL_SECONDS + 1));
    }

    @Test
    void generatedCodesAreDifferent() {
        String first = service.create("https://example.com/1", null, null).code();
        String second = service.create("https://example.com/2", null, null).code();
        assertNotEquals(first, second);
    }

    /** A clock that moves only when a test moves it. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
