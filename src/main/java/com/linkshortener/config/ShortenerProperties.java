package com.linkshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings read from application.yml (prefix "shortener").
 *
 * @param baseUrl the address people see in short links, for example http://localhost:8080
 */
@ConfigurationProperties(prefix = "shortener")
public record ShortenerProperties(String baseUrl) {
}
