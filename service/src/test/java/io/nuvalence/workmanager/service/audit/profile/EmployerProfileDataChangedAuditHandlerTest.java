package io.nuvalence.workmanager.service.audit.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.workmanager.service.domain.profile.Address;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.service.AuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmployerProfileDataChangedAuditHandlerTest {
    @Mock private AuditEventService auditEventService;
    @InjectMocks private EmployerProfileDataChangedAuditHandler auditHandler;

    @Test
    void test_publishAuditEvent_WithChanges() throws JsonProcessingException {
        Employer employer = createEmployer();

        auditHandler.handlePreUpdateState(employer);
        employer.setLegalName("New Name");
        auditHandler.handlePostUpdateState(employer);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        Map<String, String> before = new HashMap<>();
        before.put("legalName", "legalName");

        Map<String, String> after = new HashMap<>();
        after.put("legalName", "New Name");

        verify(auditEventService)
                .sendStateChangeEvent(
                        originatorId,
                        originatorId,
                        String.format("Data for employer profile %s changed.", employer.getId()),
                        employer.getId(),
                        AuditEventBusinessObject.EMPLOYER,
                        before,
                        after,
                        null,
                        AuditActivityType.EMPLOYER_PROFILE_DATA_CHANGED.getValue());
    }

    @Test
    void test_publishAuditEvent_WithNoChanges() {
        Employer employer = createEmployer();

        auditHandler.handlePreUpdateState(employer);
        auditHandler.handlePostUpdateState(employer);

        String originatorId = "originatorId";
        auditHandler.publishAuditEvent(originatorId);

        verifyNoInteractions(auditEventService);
    }

    private Employer createEmployer() {
        Address address =
                Address.builder()
                        .id(UUID.randomUUID())
                        .address1("addressLine1")
                        .address2("addressLine2")
                        .city("city")
                        .state("state")
                        .postalCode("zipCode")
                        .build();

        return Employer.builder()
                .id(UUID.randomUUID())
                .fein("fein")
                .legalName("legalName")
                .otherNames(Collections.EMPTY_LIST)
                .type("LLC")
                .industry("industry")
                .businessPhone("businessPhone")
                .summaryOfBusiness("summaryOfBusiness")
                .mailingAddress(address)
                .locations(List.of(address))
                .build();
    }
}
