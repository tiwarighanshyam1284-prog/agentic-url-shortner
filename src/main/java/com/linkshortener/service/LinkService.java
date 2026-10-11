package com.linkshortener.service;

import com.linkshortener.model.ShortLink;
import com.linkshortener.repository.LinkRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * The rules of the shortener: create a link, follow a link, count clicks, disable a link.
 * Controllers call this class. It does not know about HTTP.
 */
@Service
public class LinkService {

    static final int CODE_LENGTH = 7;
    static final int MAX_CODE_ATTEMPTS = 5;
    static final long MAX_TTL_SECONDS = 365L * 24 * 60 * 60;

    private final LinkRepository repository;
    private final CodeGenerator codes;
    private final UrlValidator validator;
    private final Clock clock;

    public LinkService(LinkRepository repository, CodeGenerator codes, UrlValidator validator, Clock clock) {
        this.repository = repository;
        this.codes = codes;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Creates a short link. With an alias, the alias is the code. Without one, a random code is made.
     *
     * @param ttlSeconds optional lifetime in seconds, at most one year; null means no expiry
     */
    public ShortLink create(String url, String alias, Long ttlSeconds) {
        String target = validator.requireTargetUrl(url);
        Instant now = clock.instant();
        Instant expiresAt = expiryFor(ttlSeconds, now);

        if (alias != null && !alias.isBlank()) {
            ShortLink link = new ShortLink(validator.normaliseAlias(alias), target, true, now, expiresAt);
            if (!repository.putIfAbsent(link)) {
                throw ApiException.conflict("alias is already in use");
            }
            return link;
        }

        // A random code is almost never taken. Trying again is cheap, so a few attempts are enough.
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            ShortLink link = new ShortLink(codes.next(CODE_LENGTH), target, false, now, expiresAt);
            if (repository.putIfAbsent(link)) {
                return link;
            }
        }
        throw ApiException.serverError("could not create a unique code, please try again");
    }

    /** Returns the link, whatever its state. Used for details and statistics. */
    public ShortLink find(String code) {
        return repository.findByCode(code).orElseThrow(() -> ApiException.notFound("no such link"));
    }

    /**
     * Returns the link to follow and counts one click.
     * A disabled or expired link is refused and its click is not counted.
     */
    public ShortLink followForRedirect(String code) {
        ShortLink link = find(code);
        Instant now = clock.instant();
        if (link.isUnavailable(now)) {
            throw ApiException.gone("link has been disabled or has expired");
        }
        link.recordClick(now);
        return link;
    }

    /** Disables the link. Calling it again changes nothing. */
    public void disable(String code) {
        find(code).disable();
    }

    private Instant expiryFor(Long ttlSeconds, Instant now) {
        if (ttlSeconds == null) {
            return null;
        }
        if (ttlSeconds < 1 || ttlSeconds > MAX_TTL_SECONDS) {
            throw ApiException.badRequest("ttlSeconds must be between 1 and " + MAX_TTL_SECONDS);
        }
        return now.plusSeconds(ttlSeconds);
    }
}
