package com.linkshortener.api.dto;

import com.linkshortener.model.ShortLink;
import java.time.Instant;

/** A link as returned to the caller. */
public record LinkResponse(
        String code,
        String shortUrl,
        String targetUrl,
        boolean customAlias,
        Instant createdAt,
        Instant expiresAt,
        boolean disabled) {

    public static LinkResponse of(ShortLink link, String baseUrl) {
        return new LinkResponse(
                link.code(),
                baseUrl + "/" + link.code(),
                link.targetUrl(),
                link.customAlias(),
                link.createdAt(),
                link.expiresAt(),
                link.disabled());
    }
}
