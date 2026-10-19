package com.itmonteur.hospitalerp.billing.internal;

import com.itmonteur.hospitalerp.appointments.Appointment;
import com.itmonteur.hospitalerp.appointments.AppointmentService;
import com.itmonteur.hospitalerp.billing.BillNotificationEvent;
import com.itmonteur.hospitalerp.common.ResourceNotFoundException;
import com.itmonteur.hospitalerp.staff.Doctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * The visit's bill (docs/BILLING_PLAN.md): made at check-in with the doctor's consultation fee; cancelled while unpaid
 * when the patient leaves the queue or the visit is cancelled (and reopened at the next check-in); marked for refund
 * when a paid visit is cancelled. Runs inside the appointment's transaction (BillingEventListener).
 */
@Service
public class BillService {

    private static final Logger logger = LoggerFactory.getLogger(BillService.class);

    private final BillRepository billRepository;
    private final FeeService feeService;
    private final AppointmentService appointmentService;
    private final BillNotifications notifications;
    private final Clock clock;

    public BillService(BillRepository billRepository, FeeService feeService, AppointmentService appointmentService,
                       BillNotifications notifications, Clock clock) {
        this.billRepository = billRepository;
        this.feeService = feeService;
        this.appointmentService = appointmentService;
        this.notifications = notifications;
        this.clock = clock;
    }

    /** At check-in: the visit's bill, made now or reopened; one that is paid (or being refunded) stays as it is. */
    @Transactional
    public void openBill(long appointmentId) {
        Bill existing = billRepository.findByAppointmentId(appointmentId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == BillStatus.CANCELLED) {
                existing.setStatus(BillStatus.UNPAID);
                logger.info("Bill {} reopened", existing.getBillNumber());
                notifications.publish(BillNotificationEvent.Kind.BILL_READY, existing);
            }
            return;
        }
        Appointment appointment = appointmentService.findAppointmentEntity(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));
        Doctor doctor = appointment.getDoctor();
        String doctorName = doctor == null ? null
                : doctor.getName() != null && !doctor.getName().isBlank() ? doctor.getName() : doctor.getUserName();

        Bill bill = new Bill();
        bill.setAppointmentId(appointmentId);
        bill.setPatientId(appointment.getPtInfo() == null ? null : appointment.getPtInfo().getPatientId());
        bill.setDoctorId(doctor == null ? null : doctor.getId());
        bill.setPatientName(appointment.getPatientName());
        bill.setDoctorName(doctorName);
        bill.setVisitDate(appointment.getDate());
        bill.setStatus(BillStatus.UNPAID);
        bill.setCreatedAt(LocalDateTime.now(clock));
        bill.addItem(doctorName == null ? "Consultation" : "Consultation — Dr. " + doctorName,
                doctor == null ? feeService.defaultFee() : feeService.feeFor(doctor.getId()), true);
        Bill saved = billRepository.save(bill);
        // The number needs the id, so it is set right after the insert (in the same transaction)
        saved.setBillNumber("B-%d-%06d".formatted(saved.getVisitDate().getYear(), saved.getId()));
        logger.info("Bill {} made for appointment {}", saved.getBillNumber(), appointmentId);
        notifications.publish(BillNotificationEvent.Kind.BILL_READY, saved);
    }

    /** The checked-in patient left the queue (check-in undone, or rescheduled): an unpaid bill is cancelled. */
    @Transactional
    public void leftQueue(long appointmentId) {
        billRepository.findByAppointmentId(appointmentId)
                .filter(bill -> bill.getStatus() == BillStatus.UNPAID)
                .ifPresent(bill -> {
                    bill.setStatus(BillStatus.CANCELLED);
                    logger.info("Bill {} cancelled: the patient left the queue", bill.getBillNumber());
                });
    }

    /** The visit was cancelled: an unpaid bill is cancelled, a paid one is due for refund. */
    @Transactional
    public void visitCancelled(long appointmentId) {
        billRepository.findByAppointmentId(appointmentId).ifPresent(bill -> {
            if (bill.getStatus() == BillStatus.UNPAID) {
                bill.setStatus(BillStatus.CANCELLED);
                logger.info("Bill {} cancelled with its visit", bill.getBillNumber());
            } else if (bill.getStatus() == BillStatus.PAID) {
                bill.setStatus(BillStatus.REFUND_DUE);
                logger.info("Bill {} due for refund: its visit was cancelled", bill.getBillNumber());
                notifications.publish(BillNotificationEvent.Kind.REFUND_DUE, bill);
            }
        });
    }
}
