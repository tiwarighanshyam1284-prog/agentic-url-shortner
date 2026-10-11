package com.linkshortener.api;

import com.linkshortener.api.dto.CreateLinkRequest;
import com.linkshortener.api.dto.LinkResponse;
import com.linkshortener.api.dto.StatsResponse;
import com.linkshortener.config.ShortenerProperties;
import com.linkshortener.model.ShortLink;
import com.linkshortener.service.LinkService;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP routes. Each method reads the request, calls LinkService, and builds the reply.
 * It holds no rules of its own.
 */
@RestController
public class LinkController {

    private final LinkService links;
    private final String baseUrl;

    public LinkController(LinkService links, ShortenerProperties properties) {
        this.links = links;
        this.baseUrl = properties.baseUrl().replaceAll("/+$", "");
    }

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    /** Creates a link. Replies 201 Created with the Location of the new link's details. */
    @PostMapping("/api/v1/links")
    public ResponseEntity<LinkResponse> create(@RequestBody CreateLinkRequest request) {
        ShortLink link = links.create(request.url(), request.alias(), request.ttlSeconds());
        return ResponseEntity
                .created(URI.create(baseUrl + "/api/v1/links/" + link.code()))
                .body(LinkResponse.of(link, baseUrl));
    }

    @GetMapping("/api/v1/links/{code}")
    public LinkResponse get(@PathVariable String code) {
        return LinkResponse.of(links.find(code), baseUrl);
    }

    @GetMapping("/api/v1/links/{code}/stats")
    public StatsResponse stats(@PathVariable String code) {
        return StatsResponse.of(links.find(code));
    }

    /** Disables a link. Replies 204 No Content, even if it was already disabled. */
    @DeleteMapping("/api/v1/links/{code}")
    public ResponseEntity<Void> disable(@PathVariable String code) {
        links.disable(code);
        return ResponseEntity.noContent().build();
    }

    /** The short address itself: 302 redirect to the long address, and one click is counted. */
    // The pattern accepts only letters, digits and hyphens. So "/" and file names such as
    // "/index.html" never reach this route, and the test page at "/" is served as a static file.
    @GetMapping("/{code:[A-Za-z0-9-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        ShortLink link = links.followForRedirect(code);
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(link.targetUrl()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
