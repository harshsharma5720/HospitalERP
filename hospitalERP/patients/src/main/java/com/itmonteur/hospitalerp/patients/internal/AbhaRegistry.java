package com.itmonteur.hospitalerp.patients.internal;

import com.itmonteur.hospitalerp.common.ConflictException;
import org.springframework.stereotype.Component;

/**
 * One ABHA number, one person (docs/ABHA_NUMBER_PLAN.md): a number may sit on one patient or one family member, never
 * on two records. Each table's unique key catches two saves at the same moment.
 */
@Component
public class AbhaRegistry {

    private static final long NONE = -1L;

    private final PtInfoRepository patients;
    private final PtRelativeRepository relatives;

    public AbhaRegistry(PtInfoRepository patients, PtRelativeRepository relatives) {
        this.patients = patients;
        this.relatives = relatives;
    }

    /** Refuses a number that is on any record but the given patient's or family member's own (null: none). */
    public void requireFree(String abhaNumber, Long ownPatientId, Long ownRelativeId) {
        if (abhaNumber == null) {
            return;
        }
        if (patients.existsByAbhaNumberAndPatientIdNot(abhaNumber, ownPatientId == null ? NONE : ownPatientId)
                || relatives.existsByAbhaNumberAndIdNot(abhaNumber, ownRelativeId == null ? NONE : ownRelativeId)) {
            throw new ConflictException("This ABHA number is already on another patient's record");
        }
    }
}
