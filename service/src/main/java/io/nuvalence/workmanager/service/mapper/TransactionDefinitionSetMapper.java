package io.nuvalence.workmanager.service.mapper;

import io.nuvalence.workmanager.service.domain.transaction.DashboardConfiguration;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetCreateModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetDashboardResultModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetResponseModel;
import io.nuvalence.workmanager.service.generated.models.TransactionDefinitionSetUpdateModel;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for transaction definition set.
 */
@Mapper(componentModel = "spring")
public interface TransactionDefinitionSetMapper extends LazyLoadingAwareMapper {
    TransactionDefinitionSetMapper INSTANCE =
            Mappers.getMapper(TransactionDefinitionSetMapper.class);

    TransactionDefinitionSetResponseModel transactionDefinitionSetToResponseModel(
            TransactionDefinitionSet value);

    TransactionDefinitionSet updateModelToTransactionDefinitionSet(
            TransactionDefinitionSetUpdateModel model);

    TransactionDefinitionSet createModelToTransactionDefinitionSet(
            TransactionDefinitionSetCreateModel model);

    default TransactionDefinitionSetDashboardResultModel dashboardToDashboardModel(
            DashboardConfiguration dashboardConfiguration) {
        return Mappers.getMapper(DashboardConfigurationMapper.class)
                .dashboardConfigurationToDashboardResultModel(dashboardConfiguration, null);
    }
}
