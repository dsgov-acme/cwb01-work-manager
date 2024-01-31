package io.nuvalence.workmanager.service.service;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.workmanager.service.domain.transaction.MissingTaskException;
import io.nuvalence.workmanager.service.domain.transaction.Transaction;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.mapper.EntityMapper;
import io.nuvalence.workmanager.service.utils.camunda.CamundaWorkflowInspector;
import io.nuvalence.workmanager.service.utils.camunda.CamundaWorkflowInspectorCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to handle task interactions on transactions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionTaskService {
    private final ProcessEngine processEngine;
    private final EntityMapper entityMapper;
    private final AuthorizationHandler authorizationHandler;
    private final CamundaWorkflowInspectorCache camundaWorkflowInspectorCache;

    /**
     * Completes the given task, posting to the workflow the data in the transaction.
     *
     * @param transaction Transaction to complete task on
     * @param taskId      ID of task to complete
     * @param action   optional workflow action passed that influences decisions in workflow
     * @throws MissingTaskException    If the process instance for this transaction does not have a task matching taskId
     */
    public void completeTask(
            final Transaction transaction, final String taskId, final String action)
            throws MissingTaskException {
        final TaskService taskService = processEngine.getTaskService();
        final Task task =
                taskService
                        .createTaskQuery()
                        .processInstanceId(transaction.getProcessInstanceId())
                        .taskDefinitionKey(taskId)
                        .list()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new MissingTaskException(transaction, taskId));

        Map<String, Object> dataMap =
                entityMapper.convertAttributesToGenericMap(transaction.getData());

        Map<String, Object> variables = new HashMap<>();
        variables.put("transactionId", transaction.getId());
        variables.put("data", dataMap);

        if (action != null) {
            variables.put("action", action);
        }
        taskService.complete(task.getId(), variables);
    }

    /**
     * Lists currently active tasks on transaction.
     *
     * @param transaction Transaction to find active tasks for
     * @return list of task names
     */
    public List<WorkflowTask> getActiveTasksForCurrentUser(final Transaction transaction) {
        final TaskService taskService = processEngine.getTaskService();
        final CamundaWorkflowInspector workflowInspector =
                camundaWorkflowInspectorCache.getByProcessInstanceId(
                        transaction.getProcessInstanceId());
        return taskService
                .createTaskQuery()
                .processInstanceId(transaction.getProcessInstanceId())
                .active()
                .list()
                .stream()
                .map(Task::getTaskDefinitionKey)
                .filter(
                        taskKey ->
                                workflowInspector.isCurrentUserAllowed(
                                        taskKey, authorizationHandler, transaction))
                .map(workflowInspector::getWorkflowTask)
                .collect(Collectors.toList());
    }

    /**
     * Start a Camunda process.
     *
     * @param transaction Transaction that starts the camunda process
     * @param processDefinitionKey Key of the camunda process definition on which the process being started is based
     */
    public void startTask(Transaction transaction, String processDefinitionKey) {
        processEngine
                .getRuntimeService()
                .startProcessInstanceByKey(
                        processDefinitionKey, Map.of("transactionId", transaction.getId()));
    }

    /**
     * Checks if the workflow associated with the given transaction has reached an end event.
     *
     * @param transaction Transaction associated with the workflow
     * @return true if the workflow has reached an end event, false otherwise
     */
    public boolean hasReachedEndEvent(final Transaction transaction) {
        final HistoryService historyService = processEngine.getHistoryService();
        String processInstanceId = transaction.getProcessInstanceId();

        HistoricProcessInstance historicProcessInstance =
                historyService
                        .createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();

        boolean hasEnded =
                historicProcessInstance != null && historicProcessInstance.getEndTime() != null;

        return hasEnded;
    }
}
