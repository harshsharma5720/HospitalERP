package com.itmonteur.hospitalerp.inbox.internal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** How the notification centre writes days, times, rupees and payment methods (docs/NOTIFICATION_CENTRE_PLAN.md). */
class TextsTest {

    @Test
    void rupeesAsTheAppWritesThemWithIndianGrouping() {
        assertThat(Texts.rupees(new BigDecimal("0"))).isEqualTo("₹0.00");
        assertThat(Texts.rupees(new BigDecimal("500"))).isEqualTo("₹500.00");
        assertThat(Texts.rupees(new BigDecimal("1250.5"))).isEqualTo("₹1,250.50");
        assertThat(Texts.rupees(new BigDecimal("125000"))).isEqualTo("₹1,25,000.00");
        assertThat(Texts.rupees(new BigDecimal("12345678.905"))).isEqualTo("₹1,23,45,678.91");
        assertThat(Texts.rupees(new BigDecimal("-500"))).isEqualTo("-₹500.00");
        assertThat(Texts.rupees(null)).isEqualTo("₹0.00");
    }

    @Test
    void daysTimesAndMethods() {
        assertThat(Texts.day("2026-10-21")).isEqualTo("Wed 21 Oct");
        assertThat(Texts.day("not a date")).isEqualTo("not a date");
        assertThat(Texts.time("10:00 - 10:30")).isEqualTo("10:00–10:30");
        assertThat(Texts.days(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 21))).isEqualTo("Wed 21 Oct");
        assertThat(Texts.days(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 17))).isEqualTo("15–17 Oct");
        assertThat(Texts.days(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 2))).isEqualTo("30 Oct – 2 Nov");
        assertThat(Texts.method("CASH")).isEqualTo("cash");
        assertThat(Texts.method("CARD")).isEqualTo("card");
        assertThat(Texts.method("UPI")).isEqualTo("UPI");
        assertThat(Texts.method("ONLINE")).isEqualTo("online payment");
    }
}
