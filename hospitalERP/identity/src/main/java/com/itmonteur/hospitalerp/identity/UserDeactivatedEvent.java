package com.itmonteur.hospitalerp.identity;

/**
 * Published by the identity module (UserService.deactivate) when an account is deactivated, inside the deactivating
 * transaction. The waitlist module ends the account's waits (docs/WAITLIST_PLAN.md). Belongs to the identity module.
 *
 * @param userId the account's id
 * @param role   its role: a patient's or a doctor's waits end
 */
public record UserDeactivatedEvent(Long userId, Role role) {
}
