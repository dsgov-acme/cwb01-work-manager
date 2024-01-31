package io.nuvalence.workmanager.service.controllers;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.exceptions.ProvidedDataException;
import io.nuvalence.workmanager.service.domain.profile.Profile;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageSender;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.generated.controllers.ConversationsApiDelegate;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.PageConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.PageConversationsResponseModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import io.nuvalence.workmanager.service.mapper.ConversationMapper;
import io.nuvalence.workmanager.service.mapper.MessageMapper;
import io.nuvalence.workmanager.service.mapper.PagingMetadataMapper;
import io.nuvalence.workmanager.service.models.ConversationFilters;
import io.nuvalence.workmanager.service.service.CommonProfileService;
import io.nuvalence.workmanager.service.service.ConversationService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;

/**
 * Implementation of the ConversationsApiDelegate interface.
 * Handles API requests related to conversations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationsApiDelegateImpl implements ConversationsApiDelegate {
    private final ConversationService conversationService;
    private final ConversationMapper conversationMapper;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final MessageMapper messageMapper;
    private final AuthorizationHandler authorizationHandler;

    private final CommonProfileService commonProfileService;
    private final TransactionService transactionService;
    private static final String CREATE_CONVERSATION_ACTION = "create-conversations";
    private static final String VIEW_CONVERSATION_ACTION = "view-conversations";
    private static final String PROFILE_HEADER_NOT_FOUND = "X-Application-Profile-ID not found";

    @Override
    public ResponseEntity<ConversationResponseModel> postConversation(
            UUID xapplicationProfileID, ConversationCreateModel conversationCreateModel) {

        Profile requestProfile =
                commonProfileService
                        .getProfileById(xapplicationProfileID)
                        .filter(
                                p ->
                                        authorizationHandler.isAllowedForInstance(
                                                CREATE_CONVERSATION_ACTION, p))
                        .orElseThrow(() -> new NotFoundException(PROFILE_HEADER_NOT_FOUND));

        Conversation conversation =
                conversationMapper.createModelToConversation(conversationCreateModel);

        EntityReference entityReference = conversation.getEntityReference();

        switch (entityReference.getType()) {
            case EMPLOYER:
                if (!entityReference.getEntityId().equals(xapplicationProfileID)) {
                    throw new ProvidedDataException(
                            "X-Application-Profile-ID must match the EMPLOYER entity id");
                }
                break;

            case TRANSACTION:
                Transaction transaction =
                        transactionService
                                .getTransactionById(entityReference.getEntityId())
                                .orElseThrow(() -> new NotFoundException("Transaction not found"));

                if (!transaction.getSubjectProfileId().equals(xapplicationProfileID)) {
                    Optional<RelatedParty> additionalParty =
                            transaction.getAdditionalParties().stream()
                                    .filter(p -> p.getProfileId().equals(xapplicationProfileID))
                                    .findAny();

                    if (additionalParty.isEmpty()) {
                        throw new NotFoundException("Transaction not found");
                    }
                }
                break;

            default:
                throw new ProvidedDataException("Invalid entity type");
        }

        Message message = messageMapper.createModelToMessage(conversationCreateModel.getMessage());
        message.setOriginalMessage(true);
        conversation.setReplies(List.of(message));

        message.setSender(createSenderFromCurrentUser(requestProfile));

        Conversation savedConversation = conversationService.saveConversation(conversation);

        ConversationResponseModel conversationResponseModel =
                conversationMapper.conversationToResponseModel(savedConversation);

        return ResponseEntity.ok(conversationResponseModel);
    }

    @Override
    public ResponseEntity<PageConversationsResponseModel> getConversations(
            String referenceType,
            UUID referenceId,
            UUID xApplicationProfileID,
            String sortBy,
            String sortOrder,
            Integer pageNumber,
            Integer pageSize) {

        Profile requestProfile =
                commonProfileService
                        .getProfileById(xApplicationProfileID)
                        .filter(
                                p ->
                                        authorizationHandler.isAllowedForInstance(
                                                VIEW_CONVERSATION_ACTION, p))
                        .orElseThrow(() -> new NotFoundException(PROFILE_HEADER_NOT_FOUND));

        List<EntityReference> entityReferences =
                conversationService.getEntityReferencesForAuthenticatedUser(
                        requestProfile.getId(), requestProfile.getProfileType());

        Page<PageConversationResponseModel> results =
                conversationService
                        .getConversationByFilters(
                                ConversationFilters.builder()
                                        .referenceType(referenceType)
                                        .referenceId(referenceId)
                                        .entityReferenceIds(
                                                entityReferences.stream()
                                                        .map(EntityReference::getId)
                                                        .toList())
                                        .pageNumber(pageNumber)
                                        .sortBy(sortBy)
                                        .pageSize(pageSize)
                                        .sortOrder(sortOrder)
                                        .build())
                        .map(conversation -> createPageConversationResponse(conversation));

        PageConversationsResponseModel response = new PageConversationsResponseModel();
        response.items(results.toList());
        response.setPagingMetadata(pagingMetadataMapper.toPagingMetadata(results));
        return ResponseEntity.status(200).body(response);
    }

    private PageConversationResponseModel createPageConversationResponse(
            Conversation conversation) {
        PageConversationResponseModel pageConversationResponseModel =
                conversationMapper.createPageConversationsResponseModel(conversation);

        Message originalMessage =
                conversation.getReplies().stream()
                        .filter(Message::isOriginalMessage)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Original message not found in conversation"));

        ResponseMessageModel responseMessageModel =
                messageMapper.messageToResponseModel(originalMessage);

        pageConversationResponseModel.setOriginalMessage(responseMessageModel);
        return pageConversationResponseModel;
    }

    private MessageSender createSenderFromCurrentUser(Profile requestProfile) {

        String userType =
                CurrentUserUtility.getCurrentUser().map(UserToken::getUserType).orElse(null);

        return MessageSender.builder()
                .userId(UUID.fromString(SecurityContextUtility.getAuthenticatedUserId()))
                .displayName(SecurityContextUtility.getAuthenticatedUserName())
                .userType(userType)
                .profileType(requestProfile.getProfileType())
                .profileId(requestProfile.getId())
                .build();
    }
}
