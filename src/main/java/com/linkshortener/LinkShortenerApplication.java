package com.linkshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Starts the URL shortener. Spring Boot creates every object the application needs,
 * finds the REST controllers, and starts the embedded web server on port 8080.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LinkShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkShortenerApplication.class, args);
    }
}
