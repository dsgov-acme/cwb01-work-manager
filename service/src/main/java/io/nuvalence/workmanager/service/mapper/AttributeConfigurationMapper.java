package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentClassifierConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.DocumentProcessingConfiguration;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.AttributeConfigurationJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.DocumentClassifierConfigurationJson;
import io.nuvalence.workmanager.service.domain.dynamicschema.jpa.DocumentProcessingConfigurationJson;
import io.nuvalence.workmanager.service.generated.models.AttributeConfigurationModel;
import io.nuvalence.workmanager.service.generated.models.DocumentClassifierConfigurationModel;
import io.nuvalence.workmanager.service.generated.models.DocumentProcessorConfigurationModel;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps attribute configuration between the following 3 forms.
 *
 * <ul>
 *     <li>API Model ({@link io.nuvalence.workmanager.service.generated.models.AttributeConfigurationModel})</li>
 *     <li>Logic Object ({@link io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration})</li>
 *     <li>Persistence
 *     ({@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.AttributeConfigurationJson})</li>
 * </ul>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AttributeConfigurationMapper {
    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.AttributeConfigurationJson} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration}.
     *
     * @param json persistence object for attribute configuration
     * @return business logic object for attribute configuration
     */
    default AttributeConfiguration attributeJsonToAttribute(final AttributeConfigurationJson json) {
        if (json instanceof DocumentProcessingConfigurationJson) {
            return toDomain((DocumentProcessingConfigurationJson) json);
        } else if (json instanceof DocumentClassifierConfigurationJson) {
            return toDomain((DocumentClassifierConfigurationJson) json);
        } else {
            return null;
        }
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.jpa.AttributeConfigurationJson}.
     *
     * @param attributeConfiguration business logic object for attribute configuration
     * @return persistence object for attribute configuration
     */
    default AttributeConfigurationJson attributeToAttributeJson(
            final AttributeConfiguration attributeConfiguration) {
        if (attributeConfiguration instanceof DocumentProcessingConfiguration) {
            return toJson((DocumentProcessingConfiguration) attributeConfiguration);
        } else if (attributeConfiguration instanceof DocumentClassifierConfiguration) {
            return toJson((DocumentClassifierConfiguration) attributeConfiguration);
        } else {
            return null;
        }
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration} to
     * {@link io.nuvalence.workmanager.service.generated.models.AttributeConfigurationModel}.
     *
     * @param attributeConfiguration business logic object for attribute configuration
     * @return API model for attribute configuration
     */
    default AttributeConfigurationModel attributeToAttributeModel(
            final AttributeConfiguration attributeConfiguration) {
        if (attributeConfiguration instanceof DocumentProcessingConfiguration) {
            return toModel((DocumentProcessingConfiguration) attributeConfiguration);
        } else if (attributeConfiguration instanceof DocumentClassifierConfiguration) {
            return toModel((DocumentClassifierConfiguration) attributeConfiguration);
        } else {
            return null;
        }
    }

    /**
     * Maps {@link io.nuvalence.workmanager.service.generated.models.AttributeConfigurationModel} to
     * {@link io.nuvalence.workmanager.service.domain.dynamicschema.AttributeConfiguration}.
     *
     * @param model API model for attribute configuration
     * @return business logic object for attribute configuration
     */
    default AttributeConfiguration attributeModelToAttribute(
            final AttributeConfigurationModel model) {
        if (model instanceof DocumentProcessorConfigurationModel) {
            return toDomain((DocumentProcessorConfigurationModel) model);
        } else if (model instanceof DocumentClassifierConfigurationModel) {
            return toDomain((DocumentClassifierConfigurationModel) model);
        } else {
            return null;
        }
    }

    DocumentProcessingConfiguration toDomain(
            DocumentProcessingConfigurationJson attributeConfiguration);

    DocumentProcessingConfiguration toDomain(
            DocumentProcessorConfigurationModel attributeConfiguration);

    DocumentClassifierConfiguration toDomain(
            DocumentClassifierConfigurationJson attributeConfiguration);

    DocumentClassifierConfiguration toDomain(
            DocumentClassifierConfigurationModel attributeConfiguration);

    DocumentProcessingConfigurationJson toJson(DocumentProcessingConfiguration constraint);

    DocumentClassifierConfigurationJson toJson(DocumentClassifierConfiguration constraint);

    DocumentProcessorConfigurationModel toModel(DocumentProcessingConfiguration constraint);

    DocumentClassifierConfigurationModel toModel(DocumentClassifierConfiguration constraint);
}
