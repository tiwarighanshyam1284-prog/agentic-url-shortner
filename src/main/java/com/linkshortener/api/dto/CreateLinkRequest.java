package com.linkshortener.api.dto;

/**
 * The JSON body of POST /api/v1/links.
 *
 * @param url        the long web address (required)
 * @param alias      an optional custom short name such as "my-docs"
 * @param ttlSeconds an optional lifetime in seconds
 */
public record CreateLinkRequest(String url, String alias, Long ttlSeconds) {
}
