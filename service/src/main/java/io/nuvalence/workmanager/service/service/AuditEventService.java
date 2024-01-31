package io.nuvalence.workmanager.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuvalence.events.brokerclient.config.PublisherProperties;
import io.nuvalence.events.event.AuditEvent;
import io.nuvalence.events.event.dto.ActivityEventData;
import io.nuvalence.events.event.dto.AuditEventDataBase;
import io.nuvalence.events.event.dto.StateChangeEventData;
import io.nuvalence.events.event.service.EventGateway;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.config.exceptions.UnexpectedException;
import io.nuvalence.workmanager.service.events.EventFactory;
import io.nuvalence.workmanager.service.events.PublisherTopic;
import io.nuvalence.workmanager.service.models.auditevents.AuditActivityType;
import io.nuvalence.workmanager.service.models.auditevents.AuditEventBusinessObject;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.PostConstruct;

/**
 * Service for managing transaction audit events.
 */
@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final EventGateway eventGateway;
    private final PublisherProperties publisherProperties;
    private final RequestContextTimestamp requestContextTimestamp;

    private String fullyQualifiedTopicName;

    @PostConstruct
    private void getFullyQualifiedTopicName() {
        if (this.fullyQualifiedTopicName == null) {
            Optional<String> fullyQualifiedTopicNameOptional =
                    publisherProperties.getFullyQualifiedTopicName(
                            PublisherTopic.AUDIT_EVENTS_RECORDING.name());

            if (!fullyQualifiedTopicNameOptional.isPresent()) {
                throw new UnexpectedException(
                        "Audit events topic not configured, topic name: "
                                + PublisherTopic.DOCUMENT_PROCESSING_INITIATION.name());
            }
            this.fullyQualifiedTopicName = fullyQualifiedTopicNameOptional.get();
        }
    }

    /**
     * Post state change events to audit service.
     *
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @param oldState state of the business object previous to event.
     * @param newState state of the business object after the event.
     * @param activityType type of event action.
     * @return Result audit event id.
     */
    public UUID sendStateChangeEvent(
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType,
            String oldState,
            String newState,
            String activityType) {

        StateChangeEventData stateChangeEventData =
                createStateChangeEventData(oldState, newState, activityType, null);

        return sendAuditEvent(
                stateChangeEventData,
                originatorId,
                userId,
                summary,
                businessObjectId,
                businessObjectType);
    }

    /**
     * Post state change events to audit service.
     *
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @param oldStateMap map state of the business object previous to event.
     * @param newStateMap map state of the business object after the event.
     * @param data data of the event in json form.
     * @param activityType type of event action.
     * @return Result audit event id.
     * @throws JsonProcessingException for possible errors converting Map states to String
     */
    public UUID sendStateChangeEvent(
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType,
            Map<String, String> oldStateMap,
            Map<String, String> newStateMap,
            String data,
            String activityType)
            throws JsonProcessingException {

        String oldState = SpringConfig.getMapper().writeValueAsString(oldStateMap);
        String newState = SpringConfig.getMapper().writeValueAsString(newStateMap);

        StateChangeEventData stateChangeEventData =
                createStateChangeEventData(oldState, newState, activityType, data);
        return sendAuditEvent(
                stateChangeEventData,
                originatorId,
                userId,
                summary,
                businessObjectId,
                businessObjectType);
    }

    /**
     * Post state change events to audit service.
     *
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @param jsonData data of the event in json form .
     * @param activityType type of activity that occurred.
     * @return Result audit event id.
     */
    public UUID sendActivityAuditEvent(
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType,
            String jsonData,
            AuditActivityType activityType) {

        ActivityEventData auditEventRequestEventData =
                createActivityEventData(jsonData, activityType);
        return sendAuditEvent(
                auditEventRequestEventData,
                originatorId,
                userId,
                summary,
                businessObjectId,
                businessObjectType);
    }

    /**
     * Post state change events to audit service.
     *
     * @param data object containing specifics of the audit evet.
     * @param originatorId id of the originator of the event.
     * @param userId is of the user involved in the event.
     * @param summary brief description of the event.
     * @param businessObjectId id of the business object involved in event.
     * @param businessObjectType type of the business object involved in event.
     * @return Result audit event id.
     */
    private UUID sendAuditEvent(
            AuditEventDataBase data,
            String originatorId,
            String userId,
            String summary,
            UUID businessObjectId,
            AuditEventBusinessObject businessObjectType) {

        AuditEvent event =
                EventFactory.createAuditEvent(
                        data, originatorId, userId, summary, businessObjectId, businessObjectType);
        event.getMetadata().setTimestamp(requestContextTimestamp.getCurrentTimestamp());

        eventGateway.publishEvent(event, this.fullyQualifiedTopicName);

        return event.getMetadata().getId();
    }

    private StateChangeEventData createStateChangeEventData(
            String oldState, String newState, String activityType, String data) {
        StateChangeEventData stateChangeEventData = new StateChangeEventData();
        stateChangeEventData.setOldState(oldState);
        stateChangeEventData.setNewState(newState);
        stateChangeEventData.setActivityType(activityType);
        if (data != null) {
            stateChangeEventData.setData(data);
        }

        return stateChangeEventData;
    }

    private ActivityEventData createActivityEventData(
            String jsonData, AuditActivityType activityType) {
        ActivityEventData activityEventData = new ActivityEventData();
        activityEventData.setData(jsonData);
        activityEventData.setActivityType(activityType.getValue());

        return activityEventData;
    }
}
