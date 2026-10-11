package com.itmonteur.hospitalerp.common;

import java.util.Locale;

/** SQL LIKE patterns that take the user's text literally: a typed {@code %} or {@code _} is no wildcard. */
public final class LikePattern {

    /** The escape character to use in {@code LIKE ... ESCAPE '!'} (not a backslash: MySQL reads it as a string escape). */
    public static final char ESCAPE = '!';

    private LikePattern() {}

    /** "50%_Off" → "%50!%!_off%": for {@code LOWER(column) LIKE :pattern ESCAPE '!'}. */
    public static String containsIgnoringCase(String text) {
        return "%" + text.toLowerCase(Locale.ROOT).replaceAll("([!%_])", "!$1") + "%";
    }
}
