package com.linkshortener.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One short link and its click counter.
 *
 * <p>The identity and target never change. Only two things change after creation:
 * the link can be disabled, and clicks are counted. Both use thread-safe fields,
 * so many requests can read and count at the same time.
 */
public final class ShortLink {

    private final String code;
    private final String targetUrl;
    private final boolean customAlias;
    private final Instant createdAt;
    private final Instant expiresAt; // null means the link never expires

    private final AtomicLong clicks = new AtomicLong();
    private volatile boolean disabled;
    private volatile Instant lastClickedAt;

    public ShortLink(String code, String targetUrl, boolean customAlias, Instant createdAt, Instant expiresAt) {
        this.code = code;
        this.targetUrl = targetUrl;
        this.customAlias = customAlias;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String code() {
        return code;
    }

    public String targetUrl() {
        return targetUrl;
    }

    public boolean customAlias() {
        return customAlias;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean disabled() {
        return disabled;
    }

    public long totalClicks() {
        return clicks.get();
    }

    public Instant lastClickedAt() {
        return lastClickedAt;
    }

    /** True when the link must not be followed any more: disabled, or past its expiry time. */
    public boolean isUnavailable(Instant now) {
        return disabled || (expiresAt != null && !now.isBefore(expiresAt));
    }

    public void recordClick(Instant at) {
        clicks.incrementAndGet();
        lastClickedAt = at;
    }

    /** Disabling twice does nothing the second time. */
    public void disable() {
        disabled = true;
    }
}
