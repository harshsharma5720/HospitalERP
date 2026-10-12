package com.itmonteur.hospitalerp.billing.internal;

import java.math.BigDecimal;
import java.util.List;

/** Every doctor with their fee (by name), and the fee used for doctors without one (BILLING_DEFAULT_FEE). */
public record FeeListDTO(BigDecimal defaultFee, List<DoctorFeeDTO> doctors) {
}
