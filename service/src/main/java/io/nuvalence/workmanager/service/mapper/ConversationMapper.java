package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.securemessaging.Conversation;
import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.generated.models.ConversationCreateModel;
import io.nuvalence.workmanager.service.generated.models.ConversationRepliesResponseModel;
import io.nuvalence.workmanager.service.generated.models.ConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.PageConversationResponseModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(
        componentModel = "spring",
        uses = {MessageMapper.class})
public interface ConversationMapper {
    ConversationMapper INSTANCE = Mappers.getMapper(ConversationMapper.class);

    @Mapping(
            target = "originalMessage",
            expression = "java(originalMessageToResponseMessageModel(conversation.getReplies()))")
    ConversationResponseModel conversationToResponseModel(Conversation conversation);

    @Mapping(target = "participants", ignore = true)
    Conversation createModelToConversation(ConversationCreateModel conversationCreateModel);

    @Mapping(target = "totalMessages", expression = "java(conversation.getReplies().size())")
    PageConversationResponseModel createPageConversationsResponseModel(Conversation conversation);

    @Mapping(target = "id", expression = "java(conversation.getId())")
    @Mapping(target = "totalMessages", expression = "java(replies.size())")
    @Mapping(target = "replies", ignore = true)
    ConversationRepliesResponseModel createConversationRepliesResponseModel(
            Conversation conversation, List<Message> replies, List<UUID> participants);

    default ResponseMessageModel originalMessageToResponseMessageModel(List<Message> messages) {
        Message message =
                messages.stream()
                        .filter(Message::isOriginalMessage)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Original message not found"));

        return MessageMapper.INSTANCE.messageToResponseModel(message);
    }
}
