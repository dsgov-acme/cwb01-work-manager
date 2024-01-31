package io.nuvalence.workmanager.service.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.auth.util.SecurityContextUtility;
import io.nuvalence.workmanager.service.config.SpringConfig;
import io.nuvalence.workmanager.service.domain.profile.Employer;
import io.nuvalence.workmanager.service.domain.profile.Individual;
import io.nuvalence.workmanager.service.domain.profile.Profile;
import io.nuvalence.workmanager.service.domain.profile.ProfileType;
import io.nuvalence.workmanager.service.domain.profile.RelatedParty;
import io.nuvalence.workmanager.service.domain.securemessaging.AgencyMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EmployerMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.securemessaging.IndividualMessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageParticipant;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageSender;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.transaction.UserType;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.CreateMessageModel;
import io.nuvalence.workmanager.service.generated.models.ReferencedEntityModel;
import io.nuvalence.workmanager.service.repository.ConversationRepository;
import io.nuvalence.workmanager.service.service.CommonProfileService;
import io.nuvalence.workmanager.service.service.ConversationService;
import io.nuvalence.workmanager.service.service.TransactionService;
import io.nuvalence.workmanager.service.utils.UserUtility;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@WithMockUser(authorities = {"wm:individual_profile_admin"})
class ConversationsApiDelegateImplTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthorizationHandler authorizationHandler;

    @MockBean private ConversationRepository repository;

    @MockBean private ConversationService conversationService;

    @MockBean private CommonProfileService commonProfileService;

    @MockBean private TransactionService transactionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);

        this.objectMapper = SpringConfig.getMapper();
    }

    @Test
    void postConversation_NoProfileFound() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.empty());

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("X-Application-Profile-ID not found"));
    }

    @Test
    void postConversation_NoProfileAccess() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        when(authorizationHandler.isAllowedForInstance("create-conversations", profile))
                .thenReturn(false);

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("X-Application-Profile-ID not found"));
    }

    @Test
    void postConversation_ProfileMismatch() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("EMPLOYER");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value(
                                        "X-Application-Profile-ID must match the EMPLOYER entity"
                                                + " id"));
    }

    @Test
    void postConversation_TransactionNotFound() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        when(transactionService.getTransactionById(
                        conversationCreateModel.getEntityReference().getEntityId()))
                .thenReturn(Optional.empty());

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Transaction not found"));
    }

    @Test
    void postConversation_TransactionNotLinkedToProfile() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        Transaction transaction = mock(Transaction.class);
        when(transaction.getSubjectProfileId()).thenReturn(UUID.randomUUID());
        when(transactionService.getTransactionById(
                        conversationCreateModel.getEntityReference().getEntityId()))
                .thenReturn(Optional.of(transaction));

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages[0]").value("Transaction not found"));
    }

    @Test
    void postConversation_InvalidEntity() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("SOMETHING_ELSE");

        UUID xapplicationProfileID = UUID.randomUUID();

        Profile profile = mock(Profile.class);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        // request
        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(conversationCreateModel))
                                .header("X-Application-Profile-ID", xapplicationProfileID))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.messages[0]")
                                .value(
                                        "Field entityReference.type is invalid. Validation pattern"
                                                + " that should be followed:"
                                                + " ^(TRANSACTION|EMPLOYER)$"));
    }

    @Test
    void postConversation_EmployerSuccess() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("EMPLOYER");

        UUID xapplicationProfileID = conversationCreateModel.getEntityReference().getEntityId();

        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(xapplicationProfileID);
        when(profile.getProfileType()).thenReturn(ProfileType.EMPLOYER);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("public");

        UUID savedId = UUID.randomUUID();
        when(conversationService.saveConversation(any()))
                .thenAnswer(
                        i -> {
                            ((Conversation) i.getArgument(0)).setId(savedId);
                            return i.getArgument(0);
                        });

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("The User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post("/api/v1/conversations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            conversationCreateModel))
                                            .header(
                                                    "X-Application-Profile-ID",
                                                    xapplicationProfileID))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ConversationResponseModel responseModel =
                    objectMapper.readValue(response, ConversationResponseModel.class);

            assertEquals(savedId, responseModel.getId());
            assertEquals("hi", responseModel.getSubject());
            assertEquals(
                    conversationCreateModel.getEntityReference().getEntityId(),
                    responseModel.getEntityReference().getEntityId());
            assertEquals("EMPLOYER", responseModel.getEntityReference().getType());
            assertEquals("hi there", responseModel.getOriginalMessage().getBody());
            assertEquals(
                    conversationCreateModel.getMessage().getAttachments().size(),
                    responseModel.getOriginalMessage().getAttachments().size());
        }
    }

    @Test
    void postConversation_TransactionSuccessSubject() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = conversationCreateModel.getEntityReference().getEntityId();

        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(xapplicationProfileID);
        when(profile.getProfileType()).thenReturn(ProfileType.EMPLOYER);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("public");

        Transaction transaction = mock(Transaction.class);
        when(transaction.getSubjectProfileId()).thenReturn(xapplicationProfileID);
        when(transactionService.getTransactionById(
                        conversationCreateModel.getEntityReference().getEntityId()))
                .thenReturn(Optional.of(transaction));

        UUID savedId = UUID.randomUUID();
        when(conversationService.saveConversation(any()))
                .thenAnswer(
                        i -> {
                            ((Conversation) i.getArgument(0)).setId(savedId);
                            return i.getArgument(0);
                        });

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("The User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post("/api/v1/conversations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            conversationCreateModel))
                                            .header(
                                                    "X-Application-Profile-ID",
                                                    xapplicationProfileID))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ConversationResponseModel responseModel =
                    objectMapper.readValue(response, ConversationResponseModel.class);

            assertEquals(savedId, responseModel.getId());
            assertEquals("hi", responseModel.getSubject());
            assertEquals(
                    conversationCreateModel.getEntityReference().getEntityId(),
                    responseModel.getEntityReference().getEntityId());
            assertEquals("TRANSACTION", responseModel.getEntityReference().getType());
            assertEquals("hi there", responseModel.getOriginalMessage().getBody());
            assertEquals(
                    conversationCreateModel.getMessage().getAttachments().size(),
                    responseModel.getOriginalMessage().getAttachments().size());
        }
    }

    @Test
    void postConversation_TransactionSuccessRelatedParty() throws Exception {

        ConversationCreateModel conversationCreateModel = createBaseConversation("TRANSACTION");

        UUID xapplicationProfileID = conversationCreateModel.getEntityReference().getEntityId();

        Profile profile = mock(Profile.class);
        when(profile.getId()).thenReturn(xapplicationProfileID);
        when(profile.getProfileType()).thenReturn(ProfileType.EMPLOYER);
        when(commonProfileService.getProfileById(xapplicationProfileID))
                .thenReturn(Optional.of(profile));

        UserToken userToken = mock(UserToken.class);
        when(userToken.getUserType()).thenReturn("public");

        Transaction transaction = mock(Transaction.class);
        when(transaction.getSubjectProfileId()).thenReturn(UUID.randomUUID());
        when(transaction.getAdditionalParties())
                .thenReturn(
                        List.of(
                                RelatedParty.builder()
                                        .profileId(xapplicationProfileID)
                                        .type(ProfileType.EMPLOYER)
                                        .build()));
        when(transactionService.getTransactionById(
                        conversationCreateModel.getEntityReference().getEntityId()))
                .thenReturn(Optional.of(transaction));

        UUID savedId = UUID.randomUUID();
        when(conversationService.saveConversation(any()))
                .thenAnswer(
                        i -> {
                            ((Conversation) i.getArgument(0)).setId(savedId);
                            return i.getArgument(0);
                        });

        try (MockedStatic<SecurityContextUtility> mockSecContext =
                        Mockito.mockStatic(SecurityContextUtility.class);
                MockedStatic<CurrentUserUtility> mockUserUtiil =
                        Mockito.mockStatic(CurrentUserUtility.class)) {

            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            mockSecContext
                    .when(SecurityContextUtility::getAuthenticatedUserName)
                    .thenReturn("The User");
            mockUserUtiil
                    .when(CurrentUserUtility::getCurrentUser)
                    .thenReturn(Optional.of(userToken));

            // request
            String response =
                    mockMvc.perform(
                                    post("/api/v1/conversations")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(
                                                            conversationCreateModel))
                                            .header(
                                                    "X-Application-Profile-ID",
                                                    xapplicationProfileID))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            ConversationResponseModel responseModel =
                    objectMapper.readValue(response, ConversationResponseModel.class);

            assertEquals(savedId, responseModel.getId());
            assertEquals("hi", responseModel.getSubject());
            assertEquals(
                    conversationCreateModel.getEntityReference().getEntityId(),
                    responseModel.getEntityReference().getEntityId());
            assertEquals("TRANSACTION", responseModel.getEntityReference().getType());
            assertEquals("hi there", responseModel.getOriginalMessage().getBody());
            assertEquals(
                    conversationCreateModel.getMessage().getAttachments().size(),
                    responseModel.getOriginalMessage().getAttachments().size());
        }
    }

    private ConversationCreateModel createBaseConversation(String entityType) throws Exception {
        String jsonConversation =
                """
        {
            "entityReference": {
                "entityId": "3a6d539a-d484-4938-93f6-915f4dbe6c41",
                "type": "%s"
            },
            "message": {
                "attachments": [
                    "75011fcd-52e9-41d6-8e71-8582d96ea645",
                    "aee9ce24-c7a3-4018-9de8-cac8b0f916c1"
                ],
                "body": "hi there"
            },
            "subject": "hi"
        }
        """
                        .formatted(entityType);

        return objectMapper.readValue(jsonConversation, ConversationCreateModel.class);
    }

    @Test
    void postConversationAgencyParticipant() throws Exception {
        ConversationCreateModel conversationCreateModel = conversationCreateModel("AGENCY");

        when(conversationService.createMessageParticipantList(any()))
                .thenReturn(createMessageParticipantList());

        String requestBodyJson = objectMapper.writeValueAsString(conversationCreateModel);

        when(repository.save(any(Conversation.class))).thenReturn(createConversation());
        when(conversationService.saveConversation(any(Conversation.class)))
                .thenReturn(createConversation());

        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postConversationEmployerParticipant() throws Exception {
        ConversationCreateModel conversationCreateModel = conversationCreateModel("EMPLOYER");

        List<MessageParticipant> participants = new ArrayList<>();
        EmployerMessageParticipant employerMessageParticipant = new EmployerMessageParticipant();
        employerMessageParticipant.setEmployer(new Employer());
        participants.add(employerMessageParticipant);

        when(conversationService.createMessageParticipantList(any())).thenReturn(participants);

        String requestBodyJson = objectMapper.writeValueAsString(conversationCreateModel);

        when(repository.save(any(Conversation.class))).thenReturn(createConversation());
        when(conversationService.saveConversation(any(Conversation.class)))
                .thenReturn(createConversation());

        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postConversationIndividualParticipant() throws Exception {
        ConversationCreateModel conversationCreateModel = conversationCreateModel("INDIVIDUAL");

        List<MessageParticipant> participants = new ArrayList<>();
        IndividualMessageParticipant individualMessageParticipant =
                new IndividualMessageParticipant();
        individualMessageParticipant.setIndividual(new Individual());
        participants.add(individualMessageParticipant);

        when(conversationService.createMessageParticipantList(any())).thenReturn(participants);

        String requestBodyJson = objectMapper.writeValueAsString(conversationCreateModel);

        when(repository.save(any(Conversation.class))).thenReturn(createConversation());
        when(conversationService.saveConversation(any(Conversation.class)))
                .thenReturn(createConversation());

        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postConversationParticipantForbidden() throws Exception {
        ConversationCreateModel conversationCreateModel = conversationCreateModel("EMPLOYER");

        when(authorizationHandler.isAllowed("create", Conversation.class)).thenReturn(false);

        String requestBodyJson = objectMapper.writeValueAsString(conversationCreateModel);

        mockMvc.perform(
                        post("/api/v1/conversations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConversationsWithIndividualProfile_Success() throws Exception {
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(eq(profileId)))
                .thenReturn(Optional.of(Individual.builder().build()));
        when(conversationService.getEntityReferencesForAuthenticatedUser(
                        eq(profileId), eq(ProfileType.INDIVIDUAL)))
                .thenReturn(
                        List.of(
                                EntityReference.builder()
                                        .id(UUID.randomUUID())
                                        .entityId(UUID.randomUUID())
                                        .type(EntityType.TRANSACTION)
                                        .build()));

        Conversation conversation = createConversation();
        List<Message> messages = List.of(createMessage());
        conversation.setReplies(messages);
        List<Conversation> conversations = Arrays.asList(conversation);
        Page<Conversation> expectedPage = new PageImpl<>(conversations);
        when(conversationService.getConversationByFilters(any())).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/conversations").header("X-Application-Profile-ID", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].totalMessages").value(1))
                .andExpect(
                        jsonPath("$.items[0].originalMessage.sender.displayName").value("John Doe"))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(1))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1));
    }

    @Test
    void getConversationsWithEmployerProfile_Success() throws Exception {
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(eq(profileId)))
                .thenReturn(Optional.of(Employer.builder().build()));
        when(conversationService.getEntityReferencesForAuthenticatedUser(
                        eq(profileId), eq(ProfileType.EMPLOYER)))
                .thenReturn(
                        List.of(
                                EntityReference.builder()
                                        .id(UUID.randomUUID())
                                        .entityId(UUID.randomUUID())
                                        .type(EntityType.TRANSACTION)
                                        .build()));

        Conversation conversation = createConversation();
        List<Message> messages = List.of(createMessage());
        conversation.setReplies(messages);
        List<Conversation> conversations = Arrays.asList(conversation);
        Page<Conversation> expectedPage = new PageImpl<>(conversations);
        when(conversationService.getConversationByFilters(any())).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/conversations").header("X-Application-Profile-ID", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isNotEmpty())
                .andExpect(jsonPath("$.items[0].subject").value("subject"))
                .andExpect(jsonPath("$.items[0].totalMessages").value(1))
                .andExpect(
                        jsonPath("$.items[0].originalMessage.sender.displayName").value("John Doe"))
                .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                .andExpect(jsonPath("$.pagingMetadata.pageSize").value(1))
                .andExpect(jsonPath("$.pagingMetadata.totalCount").value(1));
    }

    @Test
    void getConversations_NoProfileFound() throws Exception {
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(eq(profileId))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/conversations").header("X-Application-Profile-ID", profileId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getConversationsWithIndividualProfile_Unauthorized() throws Exception {
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId))
                .thenReturn(Optional.of(Individual.builder().build()));

        when(authorizationHandler.isAllowedForInstance(eq("view-conversations"), any()))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/conversations").header("X-Application-Profile-ID", profileId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getConversationsWithEmployerProfile_Unauthorized() throws Exception {
        UUID profileId = UUID.randomUUID();

        when(commonProfileService.getProfileById(profileId))
                .thenReturn(Optional.of(Employer.builder().build()));

        when(authorizationHandler.isAllowedForInstance(eq("view-conversations"), any()))
                .thenReturn(false);

        mockMvc.perform(get("/api/v1/conversations").header("X-Application-Profile-ID", profileId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Disabled
    void getConversation() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        MessageSender messageSender = new MessageSender();
        messageSender.setUserId(userId);

        Message originalMessage = new Message();
        originalMessage.setBody("body");
        originalMessage.setAttachments(List.of(UUID.randomUUID()));
        originalMessage.setTimestamp(OffsetDateTime.now());
        originalMessage.setOriginalMessage(true);

        Message replieMessage = new Message();
        replieMessage.setBody("body");
        replieMessage.setAttachments(List.of(UUID.randomUUID()));
        replieMessage.setTimestamp(OffsetDateTime.now());
        replieMessage.setOriginalMessage(false);

        List<Message> messages = new ArrayList<>();
        messages.add(originalMessage);
        messages.add(replieMessage);

        Conversation conversation = createConversation();
        conversation.setReplies(messages);

        try (MockedStatic<SecurityContextUtility> mock = mockStatic(SecurityContextUtility.class)) {

            mock.when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(UUID.randomUUID().toString());
            mockMvc.perform(
                            get(
                                    "/api/v1/conversations/"
                                            + conversationId
                                            + "?sortBy=timestamp&sortOrder=DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subject").value("subject"))
                    .andExpect(jsonPath("$.unReadMessages").value(1))
                    .andExpect(jsonPath("$.totalMessages").value(2))
                    .andExpect(jsonPath("$.pagingMetadata.pageNumber").value(0))
                    .andExpect(jsonPath("$.pagingMetadata.pageSize").value(50))
                    .andExpect(jsonPath("$.pagingMetadata.totalCount").value(0));
        }
    }

    @Test
    @Disabled
    void getConversationUnForbidden() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(authorizationHandler.isAllowed("view", Conversation.class)).thenReturn(false);

        mockMvc.perform(get("/api/v1/conversations/" + conversationId))
                .andExpect(status().isForbidden());
    }

    @Test
    @Disabled
    void getConversationNotFound() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(conversationService.getConversationById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/conversations/" + conversationId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Disabled
    void createMessage_success() throws Exception {

        UUID conversationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Conversation conversation = Conversation.builder().replies(new ArrayList<>()).build();
        when(conversationService.getConversationById(conversationId))
                .thenReturn(Optional.of(conversation));

        MessageParticipant participant = new AgencyMessageParticipant();
        participant.setId(profileId);
        when(conversationService.getConversationProfileParticipant(any(), any()))
                .thenReturn(participant);

        UUID userId = UUID.randomUUID();
        MessageSender messageSender = new MessageSender();
        messageSender.setUserId(userId);

        Message reply = new Message();
        Conversation updatedConversation = Conversation.builder().replies(List.of(reply)).build();
        when(conversationService.saveConversation(any())).thenReturn(updatedConversation);

        CreateMessageModel message = new CreateMessageModel();
        message.setBody("body");

        try (MockedStatic<UserUtility> mockedUserUtility = mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> mockedSecurityContextUtility =
                        mockStatic(SecurityContextUtility.class)) {
            mockedUserUtility
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.AGENCY.getValue());
            mockedSecurityContextUtility
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            mockMvc.perform(
                            post("/api/v1/conversations/{conversationId}/messages", conversationId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(message)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Disabled
    void createMessage_forbidden() throws Exception {
        when(authorizationHandler.isAllowed("reply", Conversation.class)).thenReturn(false);

        CreateMessageModel message = new CreateMessageModel();
        message.setBody("body");

        mockMvc.perform(
                        post("/api/v1/conversations/{conversationId}/messages", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Disabled
    void createMessage_notFound() throws Exception {
        UUID conversationId = UUID.randomUUID();

        when(conversationService.getConversationById(conversationId)).thenReturn(Optional.empty());
        ;

        CreateMessageModel message = new CreateMessageModel();
        message.setBody("body");

        try (MockedStatic<UserUtility> mocked = mockStatic(UserUtility.class)) {
            mocked.when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.AGENCY.getValue());
            mockMvc.perform(
                            post("/api/v1/conversations/{conversationId}/messages", conversationId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(message)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @Disabled
    void createMessage_userNotAgency_profileIdNotProvided() throws Exception {
        UUID conversationId = UUID.randomUUID();

        Conversation conversation = Conversation.builder().replies(new ArrayList<>()).build();
        when(conversationService.getConversationById(conversationId))
                .thenReturn(Optional.of(conversation));

        CreateMessageModel message = new CreateMessageModel();
        message.setBody("body");

        try (MockedStatic<UserUtility> mocked = mockStatic(UserUtility.class)) {
            mocked.when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            mockMvc.perform(
                            post("/api/v1/conversations/{conversationId}/messages", conversationId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(message)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @Disabled
    void createMessage_userNotAgency_profileIdProvided() throws Exception {

        UUID conversationId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Conversation conversation = Conversation.builder().replies(new ArrayList<>()).build();
        when(conversationService.getConversationById(conversationId))
                .thenReturn(Optional.of(conversation));

        MessageParticipant participant = new EmployerMessageParticipant();
        participant.setId(profileId);
        when(conversationService.getConversationProfileParticipant(any(), any()))
                .thenReturn(participant);

        UUID userId = UUID.randomUUID();
        MessageSender messageSender = new MessageSender();
        messageSender.setUserId(userId);

        Message reply = new Message();
        Conversation updatedConversation = Conversation.builder().replies(List.of(reply)).build();
        when(conversationService.saveConversation(any())).thenReturn(updatedConversation);

        CreateMessageModel message = new CreateMessageModel();
        message.setBody("body");

        try (MockedStatic<UserUtility> mockedUserUtility = mockStatic(UserUtility.class);
                MockedStatic<SecurityContextUtility> mockedSecurityContextUtility =
                        mockStatic(SecurityContextUtility.class)) {
            mockedUserUtility
                    .when(UserUtility::getAuthenticatedUserType)
                    .thenReturn(UserType.PUBLIC.getValue());
            mockedSecurityContextUtility
                    .when(SecurityContextUtility::getAuthenticatedUserId)
                    .thenReturn(userId.toString());

            mockMvc.perform(
                            post("/api/v1/conversations/{conversationId}/messages", conversationId)
                                    .header("X-Application-Profile-ID", profileId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(message)))
                    .andExpect(status().isOk());
        }
    }

    private Conversation createConversation() {
        EntityReference entityReference = new EntityReference();
        entityReference.setEntityId(UUID.randomUUID());
        entityReference.setType(EntityType.TRANSACTION);
        Conversation conversation = new Conversation();
        conversation.setSubject("subject");
        conversation.setEntityReference(entityReference);
        conversation.setReplies(List.of(new Message()));
        return conversation;
    }

    private Message createMessage() {
        Conversation conversation = new Conversation();

        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setSender(createMessageSender());
        message.setConversation(conversation);
        message.setBody("Body");
        message.setAttachments(Arrays.asList(UUID.randomUUID()));
        message.setReadBy(Arrays.asList(UUID.randomUUID()));
        message.setOriginalMessage(true);
        message.setTimestamp(OffsetDateTime.now());

        return message;
    }

    private MessageSender createMessageSender() {
        MessageSender messageSender = new MessageSender();

        messageSender.setId(UUID.randomUUID());
        messageSender.setUserId(UUID.randomUUID());
        messageSender.setDisplayName("John Doe");
        messageSender.setUserType("public");
        messageSender.setProfileId(UUID.randomUUID());
        messageSender.setProfileType(ProfileType.INDIVIDUAL);

        return messageSender;
    }

    private ConversationCreateModel conversationCreateModel(String participantType) {
        return new ConversationCreateModel()
                .subject("subject")
                .message(
                        new CreateMessageModel()
                                .body("body")
                                .attachments(List.of(UUID.randomUUID())))
                .entityReference(
                        new ReferencedEntityModel()
                                .entityId(UUID.randomUUID())
                                .type("TRANSACTION"));
    }

    private List<MessageParticipant> createMessageParticipantList() {
        List<MessageParticipant> participants = new ArrayList<>();
        AgencyMessageParticipant agencyMessageParticipant = new AgencyMessageParticipant();
        agencyMessageParticipant.setUserId(UUID.randomUUID());
        participants.add(agencyMessageParticipant);
        return participants;
    }
}
