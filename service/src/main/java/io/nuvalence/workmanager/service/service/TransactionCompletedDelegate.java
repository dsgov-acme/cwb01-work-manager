package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Service layer to manage transaction completed.
 */
@Slf4j
@RequiredArgsConstructor
@Component("TransactionCompletedDelegate")
public class TransactionCompletedDelegate implements JavaDelegate {

    private final TransactionService transactionService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable("transactionId");

        if (transactionId == null) {
            log.warn("TransactionCompletedDelegate - transactionId not found");
            return;
        }

        markTransactionAsCompleted(transactionId, execution);
    }

    private void markTransactionAsCompleted(UUID transactionId, DelegateExecution execution) {
        Optional<Transaction> transactionOptional =
                transactionService.getTransactionById(transactionId);
        if (transactionOptional.isPresent()) {
            Transaction transaction = transactionOptional.get();
            transaction.setIsCompleted(true);
            transactionService.updateTransaction(transaction);
        }
    }
}
