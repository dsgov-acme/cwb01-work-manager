package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.config.exceptions.BusinessLogicException;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSet;
import io.nuvalence.workmanager.service.domain.transaction.TransactionDefinitionSetOrder;
import io.nuvalence.workmanager.service.repository.TransactionDefinitionSetOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

/**
 * Service layer to manage transaction definition set order.
 */
@Service
@RequiredArgsConstructor
public class TransactionDefinitionSetOrderService {

    private final TransactionDefinitionSetOrderRepository repository;
    private final TransactionDefinitionSetService transactionDefinitionSetService;

    /**
     * Updates the transaction definition set order.
     *
     * @param newOrder the new order
     *
     * @throws BusinessLogicException if the transaction definition set does not exist
     */
    @Transactional
    public void updateTransactionSetKeyOrder(List<String> newOrder) {
        repository.deleteAll();

        int positionForOrdering = 1;

        for (String transactionSetKey : newOrder) {
            Optional<TransactionDefinitionSet> optionalTransactionDefinitionSet =
                    transactionDefinitionSetService.getTransactionDefinitionSet(transactionSetKey);
            if (optionalTransactionDefinitionSet.isPresent()) {
                TransactionDefinitionSet transactionDefinitionSet =
                        optionalTransactionDefinitionSet.get();
                TransactionDefinitionSetOrder transactionDefinitionSetOrder =
                        TransactionDefinitionSetOrder.builder()
                                .sortOrder(positionForOrdering++)
                                .transactionDefinitionSet(transactionDefinitionSet)
                                .build();
                repository.save(transactionDefinitionSetOrder);
            } else {
                throw new BusinessLogicException(
                        "Transaction Definition Set with key "
                                + transactionSetKey
                                + " does not exist");
            }
        }
    }

    /**
     * Gets the transaction definition set order.
     *
     * @return the transaction definition set order
     */
    public List<TransactionDefinitionSet> getTransactionDefinitionSetOrder() {
        return getSortedTransactionDefinitionSetOrders().stream()
                .map(TransactionDefinitionSetOrder::getTransactionDefinitionSet)
                .collect(Collectors.toList());
    }

    /**
     * Gets the transaction definition set order as a list of strings.
     *
     * @return the transaction definition set order as a list of strings
     */
    public List<String> getTransactionDefinitionSetOrderAsString() {
        return getSortedTransactionDefinitionSetOrders().stream()
                .map(order -> order.getTransactionDefinitionSet().getKey())
                .collect(Collectors.toList());
    }

    private List<TransactionDefinitionSetOrder> getSortedTransactionDefinitionSetOrders() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(TransactionDefinitionSetOrder::getSortOrder))
                .collect(Collectors.toList());
    }
}
