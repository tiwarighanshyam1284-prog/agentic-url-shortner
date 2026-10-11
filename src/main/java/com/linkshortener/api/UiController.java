package com.linkshortener.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the test page at /urlshortner. The page itself is static/urlshortner.html.
 * The root address "/" is deliberately not mapped, so the page is not shown there.
 */
@RestController
public class UiController {

    public static final String PAGE_PATH = "/urlshortner";

    @GetMapping(PAGE_PATH)
    public ResponseEntity<Resource> page() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("static/urlshortner.html"));
    }
}
