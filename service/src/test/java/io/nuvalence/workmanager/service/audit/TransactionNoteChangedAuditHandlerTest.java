package io.nuvalence.workmanager.service.audit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.workmanager.service.audit.transaction.TransactionNoteChangedAuditHandler;
import io.nuvalence.workmanager.service.domain.NoteType;
import io.nuvalence.workmanager.service.domain.transaction.TransactionNote;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.service.AuditEventService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionNoteChangedAuditHandlerTest {

    @Mock private AuditEventService transactionAuditEventService;

    @InjectMocks private TransactionNoteChangedAuditHandler auditHandler;

    @Test
    void testPublishAuditEvent_statusChanged() throws JsonProcessingException {

        TransactionNote note = new TransactionNote();
        note.setId(UUID.randomUUID());
        note.setTitle("title");
        note.setBody("Body");
        note.setTransactionId(UUID.randomUUID());
        note.setType(new NoteType(UUID.randomUUID(), "myType"));

        auditHandler.handlePreUpdateState(note);
        note.setBody("new body");
        auditHandler.handlePostUpdateState(note);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        ArgumentCaptor<String> dataArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(transactionAuditEventService)
                .sendStateChangeEvent(
                        eq(originatorId),
                        eq(originatorId),
                        anyString(),
                        eq(note.getTransactionId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        dataArgumentCaptor.capture(),
                        eq(AuditActivityType.NOTE_UPDATED.getValue()));

        // Extract the captured argument (HashMap)
        Map<String, String> oldStateProperties = oldStateArgumentCaptor.getValue();
        Map<String, String> newStateProperties = newStateArgumentCaptor.getValue();
        String data = dataArgumentCaptor.getValue();
        Assertions.assertEquals(1, oldStateProperties.size());
        Assertions.assertEquals(1, newStateProperties.size());
        Assertions.assertFalse(data.isEmpty());

        Assertions.assertTrue(oldStateProperties.containsKey("body"));
        Assertions.assertEquals("Body", oldStateProperties.get("body"));

        Assertions.assertTrue(newStateProperties.containsKey("body"));
        Assertions.assertEquals("new body", newStateProperties.get("body"));
    }

    @Test
    void testPublishAuditEvent_statusDidNotChange() throws JsonProcessingException {
        TransactionNote note = new TransactionNote();
        note.setId(UUID.randomUUID());
        note.setTitle("title");
        note.setBody("Body");
        note.setTransactionId(UUID.randomUUID());
        note.setType(new NoteType(UUID.randomUUID(), "myType"));

        auditHandler.handlePreUpdateState(note);
        auditHandler.handlePostUpdateState(note);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verify(transactionAuditEventService, never())
                .sendStateChangeEvent(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(Map.class),
                        any(Map.class),
                        any(),
                        any());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void testPublishAuditEvent_throws_Exception(CapturedOutput output)
            throws JsonProcessingException {

        TransactionNote note = new TransactionNote();
        note.setId(UUID.randomUUID());
        auditHandler.handlePreUpdateState(note);
        note.setBody("new body");
        auditHandler.handlePostUpdateState(note);

        String originatorId = "originatorId";
        // Capture the arguments passed to postStateChangeEvent method
        ArgumentCaptor<Map<String, String>> oldStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, String>> newStateArgumentCaptor =
                ArgumentCaptor.forClass(Map.class);

        doThrow(RuntimeException.class)
                .when(transactionAuditEventService)
                .sendStateChangeEvent(
                        eq(originatorId),
                        eq(originatorId),
                        anyString(),
                        eq(note.getId()),
                        eq(AuditEventBusinessObject.TRANSACTION),
                        oldStateArgumentCaptor.capture(),
                        newStateArgumentCaptor.capture(),
                        anyString(),
                        eq(AuditActivityType.NOTE_UPDATED.getValue()));

        auditHandler.publishAuditEvent(originatorId);
        assertTrue(
                output.getOut()
                        .contains(
                                "An unexpected exception occurred when recording audit event for"
                                        + " transaction note change in transaction "
                                        + note.getId()));
    }
}
