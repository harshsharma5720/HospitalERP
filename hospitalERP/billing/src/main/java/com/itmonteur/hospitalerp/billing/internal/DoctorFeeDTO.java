package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;

/**
 * A doctor and their consultation fee; {@code consultationFee} is null when none is set (the default fee applies).
 * {@code specialist}: e.g. "CARDIOLOGY"; {@code active}: false for a deactivated doctor.
 */
public record DoctorFeeDTO(Long doctorId, String doctorName, String specialist, boolean active, BigDecimal consultationFee) {
}
