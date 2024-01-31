package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.nuvalence.workmanager.service.audit.transaction.PriorityChangedAuditHandler;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.TransactionPriority;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.service.AuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PriorityChangedAuditHandlerTest {

    @Mock private AuditEventService transactionAuditEventService;

    @InjectMocks private PriorityChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_priorityChanged_success() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .priority(TransactionPriority.LOW)
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(TransactionPriority.MEDIUM);
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService)
                .sendStateChangeEvent(
                        originatorId,
                        originatorId,
                        "Transaction externalId priority was changed",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        TransactionPriority.LOW.name(),
                        TransactionPriority.MEDIUM.name(),
                        AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue());
    }

    @Test
    void testPublishAuditEvent_priorityChanged_beforenull() {
        Transaction transaction =
                Transaction.builder().id(UUID.randomUUID()).externalId("externalId").build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(TransactionPriority.MEDIUM);
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService)
                .sendStateChangeEvent(
                        originatorId,
                        originatorId,
                        "Transaction externalId priority was changed",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        "",
                        TransactionPriority.MEDIUM.name(),
                        AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue());
    }

    @Test
    void testPublishAuditEvent_priorityChanged_afternull() {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .priority(TransactionPriority.LOW)
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(null);
        auditHandler.handlePostUpdateState(transaction);
        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService)
                .sendStateChangeEvent(
                        originatorId,
                        originatorId,
                        "Transaction externalId priority was changed",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        TransactionPriority.LOW.name(),
                        "",
                        AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue());
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void testPublishAuditEvent_general_Exception(CapturedOutput output) {
        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID())
                        .externalId("externalId")
                        .priority(TransactionPriority.LOW)
                        .build();

        auditHandler.handlePreUpdateState(transaction);
        transaction.setPriority(TransactionPriority.MEDIUM);
        auditHandler.handlePostUpdateState(transaction);

        String originatorId = "originatorId";
        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .sendStateChangeEvent(
                        originatorId,
                        originatorId,
                        "test",
                        transaction.getId(),
                        AuditEventBusinessObject.TRANSACTION,
                        String.valueOf(TransactionPriority.LOW.getValue()),
                        String.valueOf(TransactionPriority.MEDIUM.getValue()),
                        AuditActivityType.TRANSACTION_PRIORITY_CHANGED.getValue());

        auditHandler.publishAuditEvent(originatorId);

        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " priority change in transaction "
                                        + transaction.getId()));
    }
}
