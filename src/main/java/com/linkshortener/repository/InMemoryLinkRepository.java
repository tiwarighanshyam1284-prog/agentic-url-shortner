package com.linkshortener.repository;

import com.linkshortener.model.ShortLink;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

/**
 * Keeps links in a thread-safe map. Links are lost when the application stops.
 */
@Repository
public class InMemoryLinkRepository implements LinkRepository {

    private final ConcurrentMap<String, ShortLink> links = new ConcurrentHashMap<>();

    @Override
    public boolean putIfAbsent(ShortLink link) {
        return links.putIfAbsent(link.code(), link) == null;
    }

    @Override
    public Optional<ShortLink> findByCode(String code) {
        return Optional.ofNullable(links.get(code));
    }
}
