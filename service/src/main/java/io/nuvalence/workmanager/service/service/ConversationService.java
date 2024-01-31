package io.nuvalence.workmanager.service.service;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.securemessaging.AgencyMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EmployerMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.securemessaging.IndividualMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageParticipant;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.models.ConversationParticipantsModel;
import io.nuvalence.workmanager.service.models.ConversationFilters;
import io.nuvalence.workmanager.service.repository.ConversationRepository;
import io.nuvalence.workmanager.service.repository.MessageParticipantRepository;
import io.nuvalence.workmanager.service.usermanagementapi.UserManagementService;
import io.nuvalence.workmanager.service.utils.UserUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository repository;
    private final MessageParticipantRepository messageParticipantRepository;
    private final EmployerService employerService;
    private final UserManagementService userManagementService;
    private final EmployerUserLinkService employerUserLinkService;
    private final IndividualService individualService;
    private final TransactionService transactionService;
    private final RelatedPartyService relatedPartyService;
    private final EntityReferenceService entityReferenceService;

    public Conversation saveConversation(final Conversation conversation) {
        return repository.save(conversation);
    }

    public List<EntityReference> getEntityReferencesForAuthenticatedUser(
            UUID profileId, ProfileType profileType) {
        List<Transaction> associatedTransactions = new ArrayList<>();
        List<Employer> associatedEmployers = new ArrayList<>();

        associatedTransactions.addAll(
                transactionService.getTransactionsBySubjectProfileIdAndType(
                        profileId, profileType));

        Optional<RelatedParty> optionalRelatedParty =
                relatedPartyService.getRelatedPartyByProfileIdAndType(profileId, profileType);
        if (optionalRelatedParty.isPresent()) {
            associatedTransactions.add(
                    optionalRelatedParty.get().getTransactionAdditionalParties());
        }

        if (profileType.equals(ProfileType.EMPLOYER)) {
            employerUserLinkService
                    .getEmployerByUserId(
                            UUID.fromString(SecurityContextUtility.getAuthenticatedUserId()))
                    .stream()
                    .forEach(
                            employerUserLink -> {
                                if (employerUserLink.getProfile().getId().equals(profileId)) {
                                    Optional<Employer> optionalEmployer =
                                            employerService.getEmployerById(
                                                    employerUserLink.getProfile().getId());

                                    if (optionalEmployer.isPresent()) {
                                        associatedEmployers.add(optionalEmployer.get());
                                    }
                                }
                            });
        }

        List<EntityReference> entityReferences = new ArrayList<>();

        associatedTransactions.stream()
                .forEach(
                        transaction -> {
                            entityReferences.addAll(
                                    entityReferenceService.findByEntityIdAndEntityType(
                                            transaction.getId(), EntityType.TRANSACTION));
                        });

        associatedEmployers.stream()
                .forEach(
                        employer -> {
                            entityReferences.addAll(
                                    entityReferenceService.findByEntityIdAndEntityType(
                                            employer.getId(), EntityType.EMPLOYER));
                        });

        return entityReferences;
    }

    public List<MessageParticipant> createMessageParticipantList(
            List<ConversationParticipantsModel> participantsModels) {
        List<MessageParticipant> participants = new ArrayList<>();

        for (ConversationParticipantsModel participant : participantsModels) {
            switch (participant.getType()) {
                case "EMPLOYER" -> {
                    EmployerMessageParticipant employerMessageParticipant =
                            createEmployerMessageParticipant(participant);
                    participants.add(employerMessageParticipant);
                }
                case "INDIVIDUAL" -> {
                    IndividualMessageParticipant individualMessageParticipant =
                            createIndividualMessageParticipant(participant);
                    participants.add(individualMessageParticipant);
                }
                case "AGENCY" -> {
                    AgencyMessageParticipant agencyMessageParticipant =
                            createAgencyMessageParticipant(participant);
                    participants.add(agencyMessageParticipant);
                }
                default -> throw new NotFoundException("Participant type not found");
            }
        }
        return participants;
    }

    public List<UUID> getAuthenticatedUserParticipantIds() {
        String userType = UserUtility.getAuthenticatedUserType();
        UUID userId = UUID.fromString(SecurityContextUtility.getAuthenticatedUserId());

        List<UUID> participantsId = new ArrayList<>();

        if (userType.equalsIgnoreCase("agency")) {
            participantsId.addAll(
                    getAgencyMessageParticipant(userId).stream()
                            .map(AgencyMessageParticipant::getId)
                            .toList());
        }
        participantsId.addAll(
                getEmployerMessageParticipant(userId).stream()
                        .map(EmployerMessageParticipant::getId)
                        .toList());
        participantsId.addAll(
                getIndividualMessageParticipant(userId).stream()
                        .map(IndividualMessageParticipant::getId)
                        .toList());

        return participantsId;
    }

    public Page<Conversation> getConversationByFilters(final ConversationFilters filters) {
        return repository.findAll(filters.getConversationByFilters(), filters.getPageRequest());
    }

    public List<AgencyMessageParticipant> getAgencyMessageParticipant(UUID uuid) {
        return messageParticipantRepository.findAllByUserId(uuid);
    }

    public List<EmployerMessageParticipant> getEmployerMessageParticipant(UUID uuid) {
        return employerUserLinkService.getEmployerByUserId(uuid).stream()
                .map(
                        employerUserLink ->
                                employerService.getEmployerById(
                                        employerUserLink.getProfile().getId()))
                .flatMap(
                        employerOpt ->
                                employerOpt
                                        .map(messageParticipantRepository::findAllByEmployer)
                                        .orElseGet(Collections::emptyList)
                                        .stream())
                .toList();
    }

    public List<IndividualMessageParticipant> getIndividualMessageParticipant(UUID uuid) {
        return individualService.getIndividualsByOwner(uuid).stream()
                .map(messageParticipantRepository::findAllByIndividual)
                .flatMap(List::stream)
                .toList();
    }

    public Optional<Conversation> getConversationById(UUID conversationId) {
        return repository.findById(conversationId);
    }

    private EmployerMessageParticipant createEmployerMessageParticipant(
            ConversationParticipantsModel participant) {
        EmployerMessageParticipant employerMessageParticipant = new EmployerMessageParticipant();
        Optional<Employer> employer =
                employerService.getEmployerById(participant.getParticipantId());
        employerMessageParticipant.setEmployer(
                employer.orElseThrow(() -> new NotFoundException("Employer not found")));
        return employerMessageParticipant;
    }

    private IndividualMessageParticipant createIndividualMessageParticipant(
            ConversationParticipantsModel participant) {
        IndividualMessageParticipant individualMessageParticipant =
                new IndividualMessageParticipant();
        Optional<Individual> individual =
                individualService.getIndividualById(participant.getParticipantId());
        individualMessageParticipant.setIndividual(
                individual.orElseThrow(() -> new NotFoundException("Individual not found")));
        return individualMessageParticipant;
    }

    private AgencyMessageParticipant createAgencyMessageParticipant(
            ConversationParticipantsModel participant) {
        AgencyMessageParticipant agencyMessageParticipant = new AgencyMessageParticipant();
        agencyMessageParticipant.setUserId(participant.getParticipantId());
        return agencyMessageParticipant;
    }

    public MessageParticipant getConversationProfileParticipant(
            UUID profileId, Conversation conversation) {
        UUID userId = UUID.fromString(SecurityContextUtility.getAuthenticatedUserId());
        String userType = UserUtility.getAuthenticatedUserType();

        Optional<MessageParticipant> participantOptional =
                conversation.getParticipants().stream()
                        .filter(
                                participant ->
                                        isMessageParticipantAllowedToReply(
                                                profileId, userId, participant))
                        .findFirst();

        if (participantOptional.isEmpty()) {
            if (userType.equalsIgnoreCase(UserType.AGENCY.getValue())) {
                AgencyMessageParticipant agencyMessageParticipant =
                        AgencyMessageParticipant.builder().userId(userId).build();

                conversation.getParticipants().add(agencyMessageParticipant);
                conversation = saveConversation(conversation);

                MessageParticipant savedParticipant =
                        conversation.getParticipants().stream()
                                .filter(
                                        part ->
                                                part instanceof AgencyMessageParticipant
                                                        && ((AgencyMessageParticipant) part)
                                                                .getUserId()
                                                                .equals(userId))
                                .findFirst()
                                .orElseThrow(() -> new NotFoundException("Participant not found"));

                participantOptional = Optional.of(savedParticipant);
            } else {
                throw new ForbiddenException("Sender not allowed to reply to this conversation");
            }
        }
        return participantOptional.get();
    }

    private boolean isMessageParticipantAllowedToReply(
            UUID profileId, UUID userId, MessageParticipant participant) {
        if (participant instanceof AgencyMessageParticipant) {
            return ((AgencyMessageParticipant) participant).getUserId().equals(userId);
        }

        if (participant instanceof EmployerMessageParticipant
                && ((EmployerMessageParticipant) participant)
                        .getEmployer()
                        .getId()
                        .equals(profileId)) {
            return ((EmployerMessageParticipant) participant)
                    .getEmployer().getUserLinks().stream()
                            .anyMatch(userLink -> userLink.getUserId().equals(userId));
        }

        if (participant instanceof IndividualMessageParticipant
                && ((IndividualMessageParticipant) participant)
                        .getIndividual()
                        .getId()
                        .equals(profileId)) {
            return ((IndividualMessageParticipant) participant)
                    .getIndividual().getUserLinks().stream()
                            .anyMatch(userLink -> userLink.getUserId().equals(userId));
        }

        return false;
    }
}
