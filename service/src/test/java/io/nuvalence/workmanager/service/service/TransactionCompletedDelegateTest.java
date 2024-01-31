package io.nuvalence.workmanager.service.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class TransactionCompletedDelegateTest {

    @Mock private DelegateExecution execution;

    @Mock private TransactionService transactionService;

    @InjectMocks private TransactionCompletedDelegate delegate;

    @Test
    void testExecute_HappyPath() throws Exception {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction();

        when(execution.getVariable("transactionId")).thenReturn(transactionId);
        when(transactionService.getTransactionById(transactionId))
                .thenReturn(Optional.of(transaction));

        delegate.execute(execution);

        verify(transactionService).getTransactionById(transactionId);
        verify(transactionService).updateTransaction(transaction);
        assertTrue(transaction.getIsCompleted());
    }

    @Test
    void testExecute_NullTransactionId() throws Exception {
        when(execution.getVariable("transactionId")).thenReturn(null);

        delegate.execute(execution);

        verifyNoInteractions(transactionService);
    }
}
