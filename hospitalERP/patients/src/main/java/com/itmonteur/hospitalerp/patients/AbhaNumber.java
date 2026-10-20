package com.itmonteur.hospitalerp.patients;

import com.itmonteur.hospitalerp.common.BadRequestException;

import java.util.regex.Pattern;

/**
 * The ABHA number (Ayushman Bharat Health Account, docs/ABHA_NUMBER_PLAN.md): 14 digits, kept as the digits only and
 * written "12-3456-7890-1234". Belongs to the patients module.
 */
public final class AbhaNumber {

    private static final Pattern DIGITS = Pattern.compile("\\d{14}");

    private AbhaNumber() {
    }

    /** The 14 digits of what was entered (spaces and dashes dropped); null when nothing was entered. */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String digits = input.replaceAll("[\\s-]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (!DIGITS.matcher(digits).matches()) {
            throw new BadRequestException("An ABHA number has 14 digits, e.g. 12-3456-7890-1234");
        }
        return digits;
    }

    /** "12-3456-7890-1234"; null stays null. */
    public static String format(String digits) {
        if (digits == null || digits.length() != 14) {
            return digits;
        }
        return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6, 10) + "-" + digits.substring(10);
    }
}
