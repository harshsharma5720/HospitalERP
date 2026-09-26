package com.itmonteur.hospitalerp.patients;

/**
 * Published by the patients module (PtRelativeService.deleteRelative) just before a relative
 * is deleted. The appointments module unlinks the relative from past and upcoming bookings in
 * a synchronous listener (same transaction), so the relative row can then be removed.
 * Belongs to the patients module.
 *
 * @param relativeId id of the relative about to be deleted
 */
public record RelativeDeletedEvent(Long relativeId) {
}
