package ITmonteur.example.hospitalERP.services;

import ITmonteur.example.hospitalERP.dto.DoctorScheduleDTO;
import ITmonteur.example.hospitalERP.entities.Shift;
import ITmonteur.example.hospitalERP.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoctorScheduleValidationTest {

    private static DoctorScheduleDTO entry(Shift shift, int startHour, int endHour, int minutes) {
        return new DoctorScheduleDTO(DayOfWeek.MONDAY, shift, true,
                LocalTime.of(startHour, 0), LocalTime.of(endHour, 0), minutes, false);
    }

    @Test
    void validScheduleIsAccepted() {
        assertThatCode(() -> DoctorScheduleService.validate(List.of(
                entry(Shift.MORNING, 9, 13, 15), entry(Shift.EVENING, 14, 18, 15)))).doesNotThrowAnyException();
    }

    @Test
    void startMustBeBeforeEnd() {
        assertThatThrownBy(() -> DoctorScheduleService.validate(List.of(entry(Shift.MORNING, 12, 9, 10))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("before end");
    }

    @Test
    void shiftsOfTheSameDayMustNotOverlap() {
        assertThatThrownBy(() -> DoctorScheduleService.validate(List.of(
                entry(Shift.MORNING, 9, 15, 10), entry(Shift.EVENING, 14, 18, 10))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("overlap");
    }

    @Test
    void duplicateEntriesAreRejected() {
        assertThatThrownBy(() -> DoctorScheduleService.validate(List.of(
                entry(Shift.MORNING, 9, 12, 10), entry(Shift.MORNING, 9, 12, 10))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Duplicate");
    }
}
