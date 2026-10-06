package com.itmonteur.hospitalerp.audit.internal;

import com.itmonteur.hospitalerp.audit.AuditAction;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AuditEntryWriterTest {

    private final AuditEntryRepository repository = mock(AuditEntryRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final AuditEntryWriter writer = new AuditEntryWriter(repository, transactionManager);

    private final AuditEntry entry = new AuditEntry(LocalDateTime.of(2026, 10, 5, 10, 0), 1L, "rao", "DOCTOR",
            AuditAction.CONSULTATION_VIEWED, 2L, 3L, null, "10.0.0.1");

    @Test
    void writesEachEntryInItsOwnTransaction() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        writer.write(new AuditEntryRecorded(entry));

        verify(transactionManager).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(repository).save(entry);
        verify(transactionManager).commit(any());
    }

    @Test
    void aFailedWriteNeverReachesTheCaller() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("database down"));

        assertThatCode(() -> writer.write(new AuditEntryRecorded(entry))).doesNotThrowAnyException();
        verify(transactionManager).rollback(any());
    }
}
