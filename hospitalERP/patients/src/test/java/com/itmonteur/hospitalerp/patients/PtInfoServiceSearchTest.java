package com.itmonteur.hospitalerp.patients;

import com.itmonteur.hospitalerp.audit.AuditAction;
import com.itmonteur.hospitalerp.audit.AuditLog;
import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.FileStorageService;
import com.itmonteur.hospitalerp.common.PageResult;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.identity.UserService;
import com.itmonteur.hospitalerp.patients.internal.AbhaRegistry;
import com.itmonteur.hospitalerp.patients.internal.PtInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** The one-box patient search (docs/PATIENT_SEARCH_PLAN.md, step P.1): which search each input becomes. */
class PtInfoServiceSearchTest {

    private static final Sort BY_NAME = Sort.by("patientName", "patientId");

    private PtInfoRepository repository;
    private AuditLog auditLog;
    private PtInfoService service;

    @BeforeEach
    void setUp() {
        repository = mock(PtInfoRepository.class);
        auditLog = mock(AuditLog.class);
        service = new PtInfoService(repository, mock(UserService.class), mock(CurrentUserService.class),
                mock(FileStorageService.class), auditLog, mock(AbhaRegistry.class), Clock.systemDefaultZone());
    }

    private static PtInfo patient(long id, String name, String phone) {
        PtInfo patient = new PtInfo();
        patient.setPatientId(id);
        patient.setPatientName(name);
        patient.setContactNo(phone);
        return patient;
    }

    private static List<String> names(PageResult<PtInfoDTO> result) {
        return result.entries().stream().map(PtInfoDTO::getPatientName).toList();
    }

    @Test
    void anEmptyBoxListsEveryoneByName() {
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(patient(2, "Asha", "9000011111")), PageRequest.of(1, 20), 21));

        PageResult<PtInfoDTO> result = service.searchPatients("  ", 1, 20);

        assertThat(names(result)).containsExactly("Asha");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalEntries()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(2);
        ArgumentCaptor<Pageable> asked = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(asked.capture());
        assertThat(asked.getValue()).isEqualTo(PageRequest.of(1, 20, BY_NAME));
        verify(auditLog).record(AuditAction.PATIENT_LIST_VIEWED, null, null, "page 2 of 2, 21 patients");
    }

    @Test
    void aShortNumberOrAHashIsAPatientId() {
        when(repository.findById(41L)).thenReturn(Optional.of(patient(41, "Meera Nair", "+919000011110")));

        assertThat(service.searchPatients("#41", 0, 20).entries()).extracting(PtInfoDTO::getPatientId).containsExactly(41L);
        assertThat(service.searchPatients(" 41 ", 0, 20).totalEntries()).isEqualTo(1);
        assertThat(service.searchPatients("# 99", 0, 20).entries()).isEmpty();

        // Linked to the patient found, so it shows up in that patient's audit trail
        verify(auditLog, times(2)).record(AuditAction.PATIENTS_SEARCHED, 41L, null, "by patient ID 41, 1 found");
        verify(auditLog).record(AuditAction.PATIENTS_SEARCHED, null, null, "by patient ID 99, 0 found");
        verify(repository, never()).searchByName(any(), any());
    }

    @Test
    void aPhoneNumberMatchesTheLastTenDigitsSortedByNameAndPaged() {
        when(repository.findByContactNoEndingWith("1111")).thenReturn(List.of(
                patient(3, "Kiran Rao", "(900) 001-1111"), patient(4, "Asha Rao", "+919000011111"),
                patient(5, "Other", "+919100011111")));

        PageResult<PtInfoDTO> all = service.searchPatients("+91 90000 11111", 0, 20);
        PageResult<PtInfoDTO> second = service.searchPatients("9000011111", 1, 1);

        assertThat(names(all)).containsExactly("Asha Rao", "Kiran Rao");
        assertThat(all.totalEntries()).isEqualTo(2);
        assertThat(names(second)).containsExactly("Kiran Rao");
        assertThat(second.totalPages()).isEqualTo(2);
        verify(auditLog, times(2)).record(AuditAction.PATIENTS_SEARCHED, null, null, "by phone ...1111, 2 found");
        assertThatThrownBy(() -> service.searchPatients("(900) 001", 0, 20))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("10 digits");
    }

    @Test
    void anythingElseIsPartOfTheNameWithTheTypedWildcardsTakenLiterally() {
        when(repository.searchByName(any(), any()))
                .thenReturn(new PageImpl<>(List.of(patient(6, "Meera Nair", null)), PageRequest.of(0, 20), 1));

        PageResult<PtInfoDTO> result = service.searchPatients("  MEERA   Nair ", 0, 20);
        service.searchPatients("50%_off!", 0, 20);

        assertThat(names(result)).containsExactly("Meera Nair");
        verify(repository).searchByName("%meera nair%", PageRequest.of(0, 20, BY_NAME));
        verify(repository).searchByName(eq("%50!%!_off!!%"), any());
        verify(auditLog).record(AuditAction.PATIENTS_SEARCHED, null, null, "by name \"MEERA Nair\", 1 found");
    }

    @Test
    void wrongPagesAreRefusedBeforeSearching() {
        assertThatThrownBy(() -> service.searchPatients("asha", -1, 20)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.searchPatients("asha", 0, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.searchPatients("asha", 0, 101)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository, auditLog);
    }
}
