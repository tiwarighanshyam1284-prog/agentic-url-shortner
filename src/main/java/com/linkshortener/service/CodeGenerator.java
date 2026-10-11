package com.linkshortener.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Makes random short codes from letters and digits (62 symbols).
 * SecureRandom makes the codes hard to guess, so people cannot list other users' links.
 */
@Component
public class CodeGenerator {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final SecureRandom random = new SecureRandom();

    public String next(int length) {
        char[] code = new char[length];
        for (int i = 0; i < length; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
