package com.itmonteur.hospitalerp.inbox.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** How the notification centre writes dates and times: "Tue 21 Oct", "10:00–10:30", "15–17 Oct". */
final class Texts {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private Texts() {
    }

    // "2026-10-21" -> "Tue 21 Oct"; anything else as it is
    static String day(String isoDate) {
        try {
            return LocalDate.parse(isoDate).format(DAY);
        } catch (DateTimeParseException | NullPointerException e) {
            return isoDate == null ? "" : isoDate;
        }
    }

    // "10:00 - 10:30" -> "10:00–10:30"
    static String time(String range) {
        return range == null ? "" : range.replace(" - ", "–");
    }

    // "₹1,25,000.00", as the app writes rupees (Indian grouping: the last three digits, then pairs)
    static String rupees(BigDecimal amount) {
        BigDecimal value = (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
        String plain = value.abs().toPlainString();
        String whole = plain.substring(0, plain.length() - 3);
        StringBuilder grouped = new StringBuilder();
        int head = whole.length() - 3;
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && i <= head && (head - i) % 2 == 0) {
                grouped.append(',');
            }
            grouped.append(whole.charAt(i));
        }
        return (value.signum() < 0 ? "-" : "") + "₹" + grouped + plain.substring(plain.length() - 3);
    }

    // CASH -> "cash", CARD -> "card", UPI -> "UPI", ONLINE -> "online payment"
    static String method(String method) {
        if (method == null) return "-";
        return switch (method) {
            case "CASH" -> "cash";
            case "CARD" -> "card";
            case "ONLINE" -> "online payment";
            default -> method;
        };
    }

    // One day: "Tue 21 Oct"; several: "15–17 Oct" or "30 Oct – 2 Nov"
    static String days(LocalDate start, LocalDate end) {
        if (end == null || start.equals(end)) {
            return start.format(DAY);
        }
        if (start.getMonth() == end.getMonth() && start.getYear() == end.getYear()) {
            return start.getDayOfMonth() + "–" + end.format(DAY_MONTH);
        }
        return start.format(DAY_MONTH) + " – " + end.format(DAY_MONTH);
    }
}
