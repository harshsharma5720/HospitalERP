package com.itmonteur.hospitalerp.waitlist.internal;

/** Where a wait stands (docs/WAITLIST_PLAN.md). Leaving it deletes it. Stored as text. */
public enum WaitlistStatus {
    WAITING,
    // The person booked with the doctor that day
    BOOKED,
    // The doctor's leave or deactivation, or the patient's account deactivated
    ENDED
}
