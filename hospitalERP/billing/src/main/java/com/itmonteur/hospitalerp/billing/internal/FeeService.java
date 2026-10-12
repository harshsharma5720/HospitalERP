package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.common.BadRequestException;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.identity.CurrentUserService;
import com.itmonteur.hospitalerp.staff.Doctor;
import com.itmonteur.hospitalerp.staff.DoctorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consultation fees, one per doctor, set by an admin (docs/BILLING_PLAN.md). A doctor without a fee uses the default
 * fee (BILLING_DEFAULT_FEE, 0 unless configured).
 */
@Service
public class FeeService {

    private static final Logger logger = LoggerFactory.getLogger(FeeService.class);
    public static final BigDecimal MAX_FEE = new BigDecimal("100000.00");

    private final DoctorFeeRepository feeRepository;
    private final DoctorService doctorService;
    private final CurrentUserService currentUserService;
    private final Clock clock;
    private final BigDecimal defaultFee;

    public FeeService(DoctorFeeRepository feeRepository, DoctorService doctorService,
                      CurrentUserService currentUserService, Clock clock,
                      @Value("${app.billing.default-fee:0}") BigDecimal defaultFee) {
        this.feeRepository = feeRepository;
        this.doctorService = doctorService;
        this.currentUserService = currentUserService;
        this.clock = clock;
        this.defaultFee = defaultFee.setScale(2, RoundingMode.HALF_UP);
    }

    /** Every doctor (deactivated ones too, marked) with their fee, by name. For admins. */
    @Transactional(readOnly = true)
    public FeeListDTO listFees() {
        Map<Long, BigDecimal> fees = feeRepository.findAll().stream()
                .collect(Collectors.toMap(DoctorFee::getDoctorId, DoctorFee::getConsultationFee));
        List<DoctorFeeDTO> doctors = doctorService.getAllDoctors().stream()
                .map(d -> new DoctorFeeDTO(d.getId(), d.getName() != null && !d.getName().isBlank() ? d.getName() : d.getUserName(),
                        d.getSpecialist(), d.isActive(), fees.get(d.getId())))
                .sorted(Comparator.comparing(DoctorFeeDTO::doctorName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return new FeeListDTO(defaultFee, doctors);
    }

    /** The fee for doctors without their own (BILLING_DEFAULT_FEE). */
    public BigDecimal defaultFee() {
        return defaultFee;
    }

    /** The fee a visit with this doctor starts with: theirs, or the default. */
    @Transactional(readOnly = true)
    public BigDecimal feeFor(Long doctorId) {
        return feeRepository.findById(doctorId).map(DoctorFee::getConsultationFee).orElse(defaultFee);
    }

    /** Sets a doctor's fee: 0 to 1,00,000 rupees, at most 2 decimals. */
    @Transactional
    public DoctorFeeDTO setFee(long doctorId, BigDecimal amount) {
        BigDecimal fee = checkedAmount(amount, "consultation fee");
        Doctor doctor = doctorService.findDoctorEntity(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
        DoctorFee doctorFee = feeRepository.findById(doctorId).orElseGet(() -> new DoctorFee(doctorId));
        doctorFee.setConsultationFee(fee);
        doctorFee.setUpdatedAt(LocalDateTime.now(clock));
        doctorFee.setUpdatedBy(currentUserService.getCurrentUser().getUsername());
        feeRepository.save(doctorFee);
        logger.info("Consultation fee of doctor {} set to {}", doctorId, fee);
        String name = doctor.getName() != null && !doctor.getName().isBlank() ? doctor.getName() : doctor.getUserName();
        return new DoctorFeeDTO(doctorId, name, doctor.getSpecialist() == null ? null : doctor.getSpecialist().name(),
                doctor.isAccountActive(), fee);
    }

    /** An amount of money in rupees: present, 0 to 1,00,000, at most 2 decimals; returned with 2 decimals. */
    static BigDecimal checkedAmount(BigDecimal amount, String what) {
        if (amount == null) {
            throw new BadRequestException("Please enter the " + what);
        }
        if (amount.signum() < 0 || amount.compareTo(MAX_FEE) > 0) {
            throw new BadRequestException("The " + what + " must be between 0 and 100000");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BadRequestException("The " + what + " can have at most 2 decimals (paise)");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
