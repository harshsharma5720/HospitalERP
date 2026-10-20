package com.itmonteur.hospitalerp.patients;

import com.itmonteur.hospitalerp.common.Gender;

import java.time.LocalDate;

/**
 * A walk-in patient registered at the front desk (docs/WALK_IN_REGISTRATION_PLAN.md): name, phone and gender are
 * required; date of birth, email and the ABHA number (docs/ABHA_NUMBER_PLAN.md) are optional. The record has no login
 * account.
 */
public record WalkInPatient(String name, String phone, Gender gender, LocalDate dob, String email, String abhaNumber) {

    public WalkInPatient(String name, String phone, Gender gender, LocalDate dob, String email) {
        this(name, phone, gender, dob, email, null);
    }
}
