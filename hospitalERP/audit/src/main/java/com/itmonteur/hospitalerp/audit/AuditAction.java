package com.itmonteur.hospitalerp.audit;

/**
 * What an audit entry records (docs/AUDIT_LOG_PLAN.md). Stored as text, so a new action needs no migration.
 * {@link #target()} says what an entry's target id points to.
 */
public enum AuditAction {
    // Medical records (clinical)
    CONSULTATION_VIEWED(Target.APPOINTMENT),
    CONSULTATION_SAVED(Target.APPOINTMENT),
    PRESCRIPTION_DOWNLOADED(Target.APPOINTMENT),
    MEDICAL_HISTORY_VIEWED(Target.PATIENT),
    // Patient profiles (patients)
    PATIENT_PROFILE_VIEWED(Target.PATIENT),
    PATIENT_PROFILE_UPDATED(Target.PATIENT),
    PATIENT_LIST_VIEWED(Target.NONE),
    PATIENTS_SEARCHED(Target.NONE),            // front desk: by phone number
    WALK_IN_REGISTERED(Target.PATIENT),        // front desk: a patient record without a login
    // Admin account actions (administration)
    USER_CREATED(Target.USER),
    ACCOUNT_DEACTIVATED(Target.USER),
    ACCOUNT_REACTIVATED(Target.USER),
    ACCOUNT_DELETED(Target.USER);

    /** What the target id of an entry refers to. */
    public enum Target { APPOINTMENT, PATIENT, USER, NONE }

    private final Target target;

    AuditAction(Target target) {
        this.target = target;
    }

    public Target target() {
        return target;
    }
}
