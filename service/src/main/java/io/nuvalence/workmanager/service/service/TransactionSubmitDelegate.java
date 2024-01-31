package io.nuvalence.workmanager.service.service;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.utils.RequestContextTimestamp;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;
import java.util.UUID;

/**
 * Service layer to manage transaction submit sequence flow.
 */
@RequiredArgsConstructor
@Component("TransactionSubmitDelegate")
@RequestScope
public class TransactionSubmitDelegate implements JavaDelegate {

    private static final String TRANSACTION_ID_KEY = "transactionId";

    private final TransactionService transactionService;
    private final RequestContextTimestamp requestContextTimestamp;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        UUID transactionId = (UUID) execution.getVariable(TRANSACTION_ID_KEY);

        if (transactionId != null) {
            Optional<Transaction> transactionOptional =
                    transactionService.getTransactionById(transactionId);
            if (transactionOptional.isPresent()) {
                Transaction transaction = transactionOptional.get();
                transaction.setSubmittedOn(requestContextTimestamp.getCurrentTimestamp());
                transactionService.updateTransaction(transaction);
            }
        }
    }
}
