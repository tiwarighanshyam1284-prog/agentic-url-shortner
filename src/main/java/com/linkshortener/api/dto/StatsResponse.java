package com.linkshortener.api.dto;

import com.linkshortener.model.ShortLink;
import java.time.Instant;

/** Click statistics for one link. lastClickedAt is null when nobody has clicked yet. */
public record StatsResponse(String code, String targetUrl, long totalClicks, Instant lastClickedAt) {

    public static StatsResponse of(ShortLink link) {
        return new StatsResponse(link.code(), link.targetUrl(), link.totalClicks(), link.lastClickedAt());
    }
}
