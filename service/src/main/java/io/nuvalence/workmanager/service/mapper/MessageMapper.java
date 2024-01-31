package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.securemessaging.Message;
import io.nuvalence.workmanager.service.domain.securemessaging.MessageParticipant;
import io.nuvalence.workmanager.service.generated.models.CreateMessageModel;
import io.nuvalence.workmanager.service.generated.models.ResponseMessageModel;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

    Message createModelToMessage(CreateMessageModel messageModel);

    @Mapping(source = "sender", target = "sender")
    Message createModelToMessageWithSender(
            CreateMessageModel messageModel, MessageParticipant sender);

    @AfterMapping
    default void createModelToMessageWithSender(
            @MappingTarget Message message, MessageParticipant sender) {
        message.setReadBy(List.of(sender.getId()));
    }

    ResponseMessageModel messageToResponseModel(Message message);

    @Mapping(target = "sender", ignore = true)
    ResponseMessageModel messageToResponseModel(Message message, List<UUID> participants);

    default boolean isRead(Message message, List<UUID> participants) {
        boolean isRead = false;
        for (UUID participant : participants) {
            if (message.getReadBy().contains(participant)) {
                isRead = true;
                break;
            }
        }
        return isRead;
    }
}
