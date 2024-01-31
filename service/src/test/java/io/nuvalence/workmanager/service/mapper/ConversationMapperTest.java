package io.nuvalence.workmanager.service.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityReference;
import io.nuvalence.workmanager.service.domain.securemessaging.EntityType;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ConversationMapperTest {

    private final ConversationMapper mapper = ConversationMapper.INSTANCE;

    @Test
    void testConversationToResponseModel() {
        // Create a sample Conversation instance
        Conversation conversation = new Conversation();
        conversation.setSubject("subject");
        EntityReference entityReference =
                EntityReference.builder()
                        .id(UUID.randomUUID())
                        .type(EntityType.TRANSACTION)
                        .entityId(UUID.randomUUID())
                        .build();
        conversation.setEntityReference(entityReference);
        conversation.addReply(Message.builder().originalMessage(true).build());

        // Call the mapper method
        ConversationResponseModel responseModel = mapper.conversationToResponseModel(conversation);

        // Assertions
        assertNotNull(responseModel);
    }

    @Test
    void testCreateModelToConversation() {
        // Create a sample ConversationCreateModel instance
        ConversationCreateModel createModel = new ConversationCreateModel();
        createModel.setSubject("subject");
        // Set necessary properties on createModel

        // Call the mapper method
        Conversation conversation = mapper.createModelToConversation(createModel);
        // Assertions
        assertNotNull(conversation);
    }
}
