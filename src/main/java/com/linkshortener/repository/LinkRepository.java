package com.linkshortener.repository;

import com.linkshortener.model.ShortLink;
import java.util.Optional;

/**
 * Where links are kept. The service depends only on this interface, so the
 * in-memory version can later be replaced by a database without changing the rules.
 */
public interface LinkRepository {

    /**
     * Stores the link only if its code is free.
     *
     * @return true when stored, false when the code was already taken
     */
    boolean putIfAbsent(ShortLink link);

    Optional<ShortLink> findByCode(String code);
}
