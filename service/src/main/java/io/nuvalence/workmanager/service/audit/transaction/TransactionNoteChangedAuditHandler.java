package io.nuvalence.workmanager.service.audit.transaction;

import io.nuvalence.workmanager.service.audit.AuditHandler;
import io.nuvalence.workmanager.service.audit.util.AuditMapManagementUtility;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AuditHandler that records an audit event if the status of a Transaction note has changed.
 */
@Slf4j
@RequiredArgsConstructor
public class TransactionNoteChangedAuditHandler implements AuditHandler<TransactionNote> {
    private final Map<String, String> before = new HashMap<>();
    private final Map<String, String> after = new HashMap<>();

    private UUID transactionNoteId;
    private UUID transactionId;

    private String noteData;

    private final AuditEventService transactionAuditEventService;

    @Override
    public void handlePreUpdateState(TransactionNote subject) {
        transactionNoteId = subject.getId();
        transactionId = subject.getTransactionId();
        try {
            before.putAll(BeanUtils.describe(subject));
            noteData = SpringConfig.getMapper().writeValueAsString(before);
        } catch (Exception e) {
            String errorMessage =
                    "Unable to convert pre-update state of transaction note "
                            + transactionNoteId
                            + " to a map";
            log.error(errorMessage, e);
        }
    }

    @Override
    public void handlePostUpdateState(TransactionNote subject) {
        try {
            after.putAll(BeanUtils.describe(subject));
        } catch (Exception e) {
            String errorMessage =
                    "Unable to convert post-update state of transaction note "
                            + transactionNoteId
                            + " to a map";
            log.error(errorMessage, e);
        }
    }

    @Override
    public void publishAuditEvent(String originatorId) {
        AuditMapManagementUtility.removeCommonItems(before, after);
        try {
            String eventSummary;
            if (!before.isEmpty() || !after.isEmpty()) {
                eventSummary = String.format("Transaction note %s changed", transactionNoteId);
            } else {
                return;
            }

            transactionAuditEventService.sendStateChangeEvent(
                    originatorId,
                    originatorId,
                    eventSummary,
                    transactionId,
                    AuditEventBusinessObject.TRANSACTION,
                    before,
                    after,
                    noteData,
                    AuditActivityType.NOTE_UPDATED.getValue());

        } catch (Exception e) {
            String errorMessage =
                    "An unexpected exception occurred when recording audit event for transaction"
                            + " note change in transaction "
                            + transactionNoteId;
            log.error(errorMessage, e);
        }
    }
}
