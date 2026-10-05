package com.itmonteur.hospitalerp.audit.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes audit entries once the change or read they describe is committed: AFTER_COMMIT means a
 * rolled-back change leaves no entry (fallbackExecution: without a transaction it is written right away).
 * Each entry gets its own transaction, so read-only transactions are no problem. A failure is logged and
 * swallowed: the user's request must not fail because of the audit table.
 */
@Component
public class AuditEntryWriter {

    private static final Logger logger = LoggerFactory.getLogger(AuditEntryWriter.class);

    private final AuditEntryRepository repository;
    private final TransactionTemplate newTransaction;

    public AuditEntryWriter(AuditEntryRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void write(AuditEntryRecorded event) {
        try {
            newTransaction.executeWithoutResult(status -> repository.save(event.entry()));
        } catch (RuntimeException e) {
            logger.error("Audit entry could not be written: {}", event.entry(), e);
        }
    }
}
