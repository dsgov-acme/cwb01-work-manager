package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.record.Record;
import io.nuvalence.workmanager.service.generated.models.RecordResponseModel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting between {@link Record} and {@link RecordResponseModel}.
 */
@Mapper(componentModel = "spring")
@AllArgsConstructor
@NoArgsConstructor
public abstract class RecordMapper {

    @Setter protected EntityMapper entityMapper;

    @Mapping(
            target = "recordDefinitionName",
            expression = "java(record.getRecordDefinition().getName())")
    @Mapping(target = "createdFrom", expression = "java(record.getCreatedFrom().getId())")
    @Mapping(
            target = "recordDefinitionId",
            expression = "java(record.getRecordDefinition().getId())")
    @Mapping(target = "lastUpdatedFrom", expression = "java(record.getLastUpdatedFrom().getId())")
    @Mapping(
            target = "data",
            expression = "java(entityMapper.convertAttributesToGenericMap(record.getData()))")
    public abstract RecordResponseModel recordToRecordResponseModel(Record record);
}
