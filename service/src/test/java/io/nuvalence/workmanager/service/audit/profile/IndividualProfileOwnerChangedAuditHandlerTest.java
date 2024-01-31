package io.nuvalence.workmanager.service.audit.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.service.AuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class IndividualProfileOwnerChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private IndividualProfileOwnerChangedAuditHandler auditHandler;

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        UUID ownerUserIdOld = UUID.randomUUID();
        Individual individual =
                Individual.builder().id(UUID.randomUUID()).ownerUserId(ownerUserIdOld).build();

        auditHandler.handlePreUpdateState(individual);

        UUID ownerUserIdNew = UUID.randomUUID();
        individual.setOwnerUserId(ownerUserIdNew);
        auditHandler.handlePostUpdateState(individual);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put("ownerUserId", ownerUserIdOld.toString());

        Map<String, String> after = new HashMap<>();
        after.put("ownerUserId", ownerUserIdNew.toString());

        verify(auditEventService)
                .sendStateChangeEvent(
                        originatorId,
                        originatorId,
                        String.format(
                                "Owner changed to [%s] for individual profile %s. Previously it was"
                                        + " owned by [%s]",
                                ownerUserIdNew, individual.getId(), ownerUserIdOld),
                        individual.getId(),
                        AuditEventBusinessObject.INDIVIDUAL,
                        ownerUserIdOld.toString(),
                        ownerUserIdNew.toString(),
                        AuditActivityType.INDIVIDUAL_PROFILE_OWNER_CHANGED.getValue());
    }

    @Test
    void test_publishAuditEvent_WithNoChanges() {
        Individual individual = Individual.builder().ownerUserId(UUID.randomUUID()).build();

        auditHandler.handlePreUpdateState(individual);
        auditHandler.handlePostUpdateState(individual);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }
}
