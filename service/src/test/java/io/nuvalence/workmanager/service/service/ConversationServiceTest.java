package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.EmployerUserLink;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.IndividualUserLink;
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
import io.nuvalence.workmanager.service.generated.models.ConversationParticipantsModel;
import io.nuvalence.workmanager.service.models.ConversationFilters;
import io.nuvalence.workmanager.service.repository.ConversationRepository;
import io.nuvalence.workmanager.service.repository.MessageParticipantRepository;
import io.nuvalence.workmanager.service.utils.UserUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

class ConversationServiceTest {

    @Mock private ConversationRepository repository;
    @Mock private MessageParticipantRepository messageParticipantRepository;

    @Mock private EmployerService employerService;
    @Mock private IndividualService individualService;
    @Mock private EmployerUserLinkService employerUserLinkService;

    @Mock private TransactionService transactionService;

    @Mock private RelatedPartyService relatedPartyService;

    @Mock private EntityReferenceService entityReferenceService;

    @InjectMocks private ConversationService conversationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveConversation() {
        Conversation conversation = new Conversation();
        when(repository.save(any(Conversation.class))).thenReturn(conversation);

        Conversation savedConversation = conversationService.saveConversation(new Conversation());
        assertNotNull(savedConversation);
        assertEquals(conversation, savedConversation);

        verify(repository, times(1)).save(any(Conversation.class));
    }

    @Test
    void testCreateMessageParticipantList() {
        UUID employerId = UUID.randomUUID();
        UUID individualId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        when(employerService.getEmployerById(any())).thenReturn(Optional.of(new Employer()));
        when(individualService.getIndividualById(any())).thenReturn(Optional.of(new Individual()));

        List<ConversationParticipantsModel> participantsModels = new ArrayList<>();
        participantsModels.add(new ConversationParticipantsModel("EMPLOYER", employerId));
        participantsModels.add(new ConversationParticipantsModel("INDIVIDUAL", individualId));
        participantsModels.add(new ConversationParticipantsModel("AGENCY", agencyId));

        List<MessageParticipant> participants =
                conversationService.createMessageParticipantList(participantsModels);

        assertEquals(3, participants.size());
        assertTrue(participants.get(0) instanceof EmployerMessageParticipant);
        assertTrue(participants.get(1) instanceof IndividualMessageParticipant);
        assertTrue(participants.get(2) instanceof AgencyMessageParticipant);

        verify(employerService, times(1)).getEmployerById(employerId);
        verify(individualService, times(1)).getIndividualById(individualId);
    }

    @Test
    void testCreateMessageParticipantList_WithEmployerNotFoundException() {
        UUID employerId = UUID.randomUUID();
        when(employerService.getEmployerById(any())).thenReturn(Optional.empty());

        List<ConversationParticipantsModel> participantsModels = new ArrayList<>();
        participantsModels.add(new ConversationParticipantsModel("EMPLOYER", employerId));

        assertThrows(
                NotFoundException.class,
                () -> conversationService.createMessageParticipantList(participantsModels));

        verify(employerService, times(1)).getEmployerById(employerId);
    }

    @Test
    void testCreateMessageParticipantList_WithIndividualNotFoundException() {
        UUID individualId = UUID.randomUUID();
        when(individualService.getIndividualById(any())).thenReturn(Optional.empty());

        List<ConversationParticipantsModel> participantsModels = new ArrayList<>();
        participantsModels.add(new ConversationParticipantsModel("INDIVIDUAL", individualId));

        assertThrows(
                NotFoundException.class,
                () -> conversationService.createMessageParticipantList(participantsModels));

        verify(individualService, times(1)).getIndividualById(individualId);
    }

    @Test
    void testGetAgencyMessageParticipant() {
        UUID uuid = UUID.randomUUID();

        // Mock repository behavior
        when(messageParticipantRepository.findAllByUserId(uuid))
                .thenReturn(List.of(new AgencyMessageParticipant()));

        // Call the method
        List<AgencyMessageParticipant> result =
                conversationService.getAgencyMessageParticipant(uuid);

        // Assertions
        assertFalse(result.isEmpty());

        // Verify repository method invocation
        verify(messageParticipantRepository, times(1)).findAllByUserId(uuid);
    }

    @Test
    void testGetEmployerMessageParticipant() {
        UUID uuid = UUID.randomUUID();

        Employer employer = new Employer();
        employer.setId(uuid);

        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setProfile(employer);

        // Mock service and repository behavior
        when(employerService.getEmployerById(uuid)).thenReturn(Optional.of(employer));
        when(employerUserLinkService.getEmployerByUserId(any()))
                .thenReturn(List.of(employerUserLink));
        when(messageParticipantRepository.findAllByEmployer(any()))
                .thenReturn(
                        List.of(
                                EmployerMessageParticipant.builder()
                                        .employer(new Employer())
                                        .build()));

        // Call the method
        List<EmployerMessageParticipant> result =
                conversationService.getEmployerMessageParticipant(uuid);

        assertFalse(result.isEmpty());

        // Verify service and repository method invocation
        verify(employerService, times(1)).getEmployerById(uuid);
        verify(messageParticipantRepository, times(1)).findAllByEmployer(any());
    }

    @Test
    void testGetIndividualMessageParticipant() {
        UUID uuid = UUID.randomUUID();
        Individual individual = new Individual();
        individual.setId(uuid);

        // Mock service and repository behavior
        when(individualService.getIndividualsByOwner(uuid)).thenReturn(List.of(individual));
        when(messageParticipantRepository.findAllByIndividual(any()))
                .thenReturn(
                        List.of(
                                IndividualMessageParticipant.builder()
                                        .individual(individual)
                                        .build()));

        // Call the method
        List<IndividualMessageParticipant> result =
                conversationService.getIndividualMessageParticipant(uuid);

        // Assertions
        assertFalse(result.isEmpty());

        // Verify service and repository method invocation
        verify(individualService, times(1)).getIndividualsByOwner(uuid);
        verify(messageParticipantRepository, times(1)).findAllByIndividual(any());
    }

    @Test
    void testGetParticipantId() {

        AgencyMessageParticipant agencyMessageParticipant =
                new AgencyMessageParticipant(UUID.randomUUID());
        when(messageParticipantRepository.findAllByUserId(any()))
                .thenReturn(List.of(agencyMessageParticipant));

        Employer employer = new Employer();
        EmployerUserLink employerUserLink = new EmployerUserLink();
        employerUserLink.setProfile(employer);
        EmployerMessageParticipant employerMessageParticipant =
                new EmployerMessageParticipant(employer);
        when(employerUserLinkService.getEmployerByUserId(any()))
                .thenReturn(List.of(employerUserLink));
        when(employerService.getEmployerById(any())).thenReturn(Optional.of(employer));
        when(messageParticipantRepository.findAllByEmployer(any()))
                .thenReturn(List.of(employerMessageParticipant));
        when(conversationService.getEmployerMessageParticipant(any()))
                .thenReturn(List.of(employerMessageParticipant));

        Individual individual = new Individual();
        IndividualMessageParticipant individualMessageParticipant =
                new IndividualMessageParticipant(individual);
        when(individualService.getIndividualsByOwner(any())).thenReturn(List.of(individual));
        when(messageParticipantRepository.findAllByIndividual(any()))
                .thenReturn(List.of(individualMessageParticipant));
        when(conversationService.getIndividualMessageParticipant(any()))
                .thenReturn(List.of(individualMessageParticipant));

        try (MockedStatic<SecurityContextUtility> mockedSecurityContextUtility =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> mockedUserUtility =
                        Mockito.mockStatic(UserUtility.class)) {
            mockedSecurityContextUtility
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(String.valueOf(UUID.randomUUID()));
            mockedUserUtility.when(UserUtility::getAuthenticatedUserType).thenReturn("agency");

            // Call the method
            List<UUID> participantsId = conversationService.getAuthenticatedUserParticipantIds();

            // Assertions
            assertNotNull(participantsId);
            assertFalse(participantsId.isEmpty());
            assertEquals(3, participantsId.size());

            // Verify method invocations
            verify(employerService, times(2)).getEmployerById(any());
            verify(individualService, times(2)).getIndividualsByOwner(any());
            verify(messageParticipantRepository, times(1)).findAllByIndividual(any());
            verify(messageParticipantRepository, times(1)).findAllByUserId(any());
            verify(messageParticipantRepository, times(1)).findAllByEmployer(any());
        }
    }

    @Test
    void getConversationsByFilters() {
        Conversation conversation = Conversation.builder().id(UUID.randomUUID()).build();
        Page<Conversation> conversationPage =
                new PageImpl<>(Collections.singletonList(conversation));

        when(repository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(conversationPage);

        Page<Conversation> conversationPageResult =
                conversationService.getConversationByFilters(
                        ConversationFilters.builder()
                                .sortBy("legalName")
                                .sortOrder("ASC")
                                .pageNumber(0)
                                .pageSize(10)
                                .entityReferenceIds(List.of(UUID.randomUUID()))
                                .build());

        assertEquals(conversationPage, conversationPageResult);
    }

    @Test
    void testGetEntityReferencesForAuthenticatedUser() {
        try (MockedStatic<SecurityContextUtility> mock = mockStatic(SecurityContextUtility.class)) {
            UUID profileId = UUID.randomUUID();
            ProfileType profileType = ProfileType.EMPLOYER;

            UUID transactionWhereSubjectId = UUID.randomUUID();
            Transaction transactionWhereSubject =
                    Transaction.builder().id(transactionWhereSubjectId).build();
            when(transactionService.getTransactionsBySubjectProfileIdAndType(
                            eq(profileId), eq(profileType)))
                    .thenReturn(List.of(transactionWhereSubject));

            UUID transactionWhereRelatedPartyId = UUID.randomUUID();
            Transaction transactionWhereRelatedParty =
                    Transaction.builder().id(transactionWhereRelatedPartyId).build();
            RelatedParty relatedParty =
                    RelatedParty.builder()
                            .transactionAdditionalParties(transactionWhereRelatedParty)
                            .build();
            when(relatedPartyService.getRelatedPartyByProfileIdAndType(
                            eq(profileId), eq(profileType)))
                    .thenReturn(Optional.ofNullable(relatedParty));

            UUID userId = UUID.randomUUID();
            mock.when(SecurityContextUtility::getAuthenticatedUserId).thenReturn(userId.toString());

            Employer employer = Employer.builder().id(profileId).build();
            EmployerUserLink employerUserLink =
                    EmployerUserLink.builder().profile(employer).userId(userId).build();
            when(employerUserLinkService.getEmployerByUserId(eq(userId)))
                    .thenReturn(List.of(employerUserLink));
            when(employerService.getEmployerById(eq(profileId)))
                    .thenReturn(Optional.ofNullable(employer));

            UUID entityReferenceOneId = UUID.randomUUID();
            when(entityReferenceService.findByEntityIdAndEntityType(
                            eq(transactionWhereSubjectId), eq(EntityType.TRANSACTION)))
                    .thenReturn(
                            List.of(EntityReference.builder().id(entityReferenceOneId).build()));
            UUID entityReferenceTwoId = UUID.randomUUID();
            when(entityReferenceService.findByEntityIdAndEntityType(
                            eq(transactionWhereRelatedPartyId), eq(EntityType.TRANSACTION)))
                    .thenReturn(
                            List.of(EntityReference.builder().id(entityReferenceTwoId).build()));
            UUID entityReferenceThreeId = UUID.randomUUID();
            when(entityReferenceService.findByEntityIdAndEntityType(
                            eq(profileId), eq(EntityType.EMPLOYER)))
                    .thenReturn(
                            List.of(EntityReference.builder().id(entityReferenceThreeId).build()));

            List<EntityReference> result =
                    conversationService.getEntityReferencesForAuthenticatedUser(
                            profileId, profileType);

            assertEquals(3, result.size());
        }
    }

    @ParameterizedTest
    @CsvSource({"true, agency", "false, agency", "false, public"})
    void testGetConversationProfileParticipant(boolean existingParticipant, String userType) {

        UUID userId = UUID.randomUUID();

        Conversation savedConversation = new Conversation();
        AgencyMessageParticipant agencyMessageParticipant =
                AgencyMessageParticipant.builder().userId(userId).build();
        savedConversation.setParticipants(List.of(agencyMessageParticipant));
        when(repository.save(any(Conversation.class))).thenReturn(savedConversation);

        try (MockedStatic<SecurityContextUtility> utility =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<UserUtility> userUtility = Mockito.mockStatic(UserUtility.class)) {
            utility.when(() -> SecurityContextUtility.getAuthenticatedUserId())
                    .thenReturn(userId.toString());
            userUtility.when(() -> UserUtility.getAuthenticatedUserType()).thenReturn(userType);

            Conversation conversation = new Conversation();
            if (existingParticipant) {
                conversation.setParticipants(List.of(agencyMessageParticipant));
            } else {
                conversation.setParticipants(new ArrayList<>());
            }

            if (!existingParticipant && userType.equals("public")) {
                assertThrows(
                        ForbiddenException.class,
                        () -> {
                            conversationService.getConversationProfileParticipant(
                                    UUID.randomUUID(), conversation);
                        },
                        "Sender not allowed to reply to this conversation");
            } else {
                MessageParticipant participant =
                        conversationService.getConversationProfileParticipant(
                                UUID.randomUUID(), conversation);

                assertTrue(participant instanceof AgencyMessageParticipant);
                assertEquals(userId, ((AgencyMessageParticipant) participant).getUserId());
                assertEquals(1, conversation.getParticipants().size());
                assertEquals(
                        userId,
                        ((AgencyMessageParticipant) conversation.getParticipants().get(0))
                                .getUserId());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("participantProvider")
    void testGetConversationProfileParticipant_whenUserAllowedToReply(
            MessageParticipant participant, UUID profileId, UUID userId) {
        Conversation conversation = new Conversation();

        conversation.setParticipants(List.of(participant));

        try (MockedStatic<SecurityContextUtility> utility =
                Mockito.mockStatic(SecurityContextUtility.class)) {

            utility.when(() -> SecurityContextUtility.getAuthenticatedUserId())
                    .thenReturn(userId.toString());

            EmployerUserLink link = new EmployerUserLink();
            link.setUserId(userId);

            when(employerUserLinkService.getEmployerByUserId(userId)).thenReturn(List.of(link));

            MessageParticipant participantResult =
                    conversationService.getConversationProfileParticipant(profileId, conversation);

            assertNotNull(participantResult);
            if (participant instanceof EmployerMessageParticipant) {
                assertEquals(
                        profileId,
                        ((EmployerMessageParticipant) participantResult).getEmployer().getId());
            } else if (participant instanceof IndividualMessageParticipant) {
                assertEquals(
                        profileId,
                        ((IndividualMessageParticipant) participantResult).getIndividual().getId());
            } else if (participant instanceof AgencyMessageParticipant) {
                assertEquals(userId, ((AgencyMessageParticipant) participantResult).getUserId());
            }
        }
    }

    static Stream<Arguments> participantProvider() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        EmployerMessageParticipant employerMessageParticipant = new EmployerMessageParticipant();
        employerMessageParticipant.setEmployer(
                Employer.builder()
                        .id(profileId)
                        .userLinks(List.of(EmployerUserLink.builder().userId(userId).build()))
                        .build());

        IndividualMessageParticipant individualMessageParticipant =
                new IndividualMessageParticipant();
        individualMessageParticipant.setIndividual(
                Individual.builder()
                        .id(profileId)
                        .userLinks(List.of(IndividualUserLink.builder().userId(userId).build()))
                        .build());

        AgencyMessageParticipant agencyMessageParticipant = new AgencyMessageParticipant();
        agencyMessageParticipant.setUserId(userId);

        return Stream.of(
                Arguments.of(employerMessageParticipant, profileId, userId),
                Arguments.of(individualMessageParticipant, profileId, userId),
                Arguments.of(agencyMessageParticipant, profileId, userId));
    }
}
