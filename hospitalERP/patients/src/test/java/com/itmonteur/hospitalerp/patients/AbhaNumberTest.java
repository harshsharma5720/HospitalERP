package com.itmonteur.hospitalerp.patients;

import com.itmonteur.hospitalerp.common.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The ABHA number's 14 digits (docs/ABHA_NUMBER_PLAN.md). */
class AbhaNumberTest {

    @Test
    void theDigitsAreKeptWhateverTheWayTheyAreWritten() {
        assertThat(AbhaNumber.normalize("12-3456-7890-1234")).isEqualTo("12345678901234");
        assertThat(AbhaNumber.normalize(" 12 3456 7890 1234 ")).isEqualTo("12345678901234");
        assertThat(AbhaNumber.normalize("12345678901234")).isEqualTo("12345678901234");
        assertThat(AbhaNumber.normalize(null)).isNull();
        assertThat(AbhaNumber.normalize("")).as("none").isNull();
        assertThat(AbhaNumber.normalize(" - ")).isNull();
    }

    @Test
    void anythingButFourteenDigitsIsRefused() {
        for (String wrong : new String[]{"1234567890123", "123456789012345", "12-3456-7890-123a", "12.3456.7890.1234",
                "١٢٣٤٥٦٧٨٩٠١٢٣٤"}) {
            assertThatThrownBy(() -> AbhaNumber.normalize(wrong)).as(wrong).isInstanceOf(BadRequestException.class)
                    .hasMessage("An ABHA number has 14 digits, e.g. 12-3456-7890-1234");
        }
    }

    @Test
    void writtenInFourGroups() {
        assertThat(AbhaNumber.format("12345678901234")).isEqualTo("12-3456-7890-1234");
        assertThat(AbhaNumber.format(null)).isNull();
    }
}
