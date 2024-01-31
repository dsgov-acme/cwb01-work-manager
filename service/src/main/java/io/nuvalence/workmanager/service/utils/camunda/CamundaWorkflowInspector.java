package io.nuvalence.workmanager.service.utils.camunda;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowAction;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import io.nuvalence.workmanager.service.utils.auth.CurrentUserUtility;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wraps a BpmnModelInstance and provides methods to inspect the model for custom workflow extensions.
 */
public class CamundaWorkflowInspector {
    public static final String ACTIONS_EXTENSION_PROPERTY = "workflow.actions";
    public static final String ACTION_LABEL_EXTENSION_PROPERTY = "workflow.action.%s.label";
    public static final String ACTION_CLASS_EXTENSION_PROPERTY = "workflow.action.%s.class";
    public static final String ACTION_MODAL_EXTENSION_PROPERTY = "workflow.action.%s.modal";
    public static final String ALLOWED_USER_TYPES_EXTENSION_PROPERTY = "workflow.allowed.userTypes";
    public static final String ALLOWED_ACTION_EXTENSION_PROPERTY = "workflow.allowed.action";

    private static final List<String> DEFAULT_ALLOWED_USER_TYPES = List.of("agency", "public");

    private static final WorkflowAction DEFAULT_ACTION =
            WorkflowAction.builder()
                    .key("Submit")
                    .uiLabel("Submit")
                    .uiClass(WorkflowAction.UI_CLASS_PRIMARY)
                    .build();

    private final BpmnModelInstance modelInstance;
    private final Map<String, UserTask> userTasks;

    /**
     * Creates a new inspector for the given model instance.
     *
     * @param modelInstance model instance to inspect
     */
    public CamundaWorkflowInspector(BpmnModelInstance modelInstance) {
        this.modelInstance = modelInstance;
        this.userTasks = new HashMap<>();
        modelInstance
                .getModelElementsByType(UserTask.class)
                .forEach(
                        userTask -> {
                            userTasks.put(userTask.getId(), userTask);
                        });
    }

    /**
     * Gets the workflow task with the given key.
     *
     * @param taskKey key of the task to retrieve
     * @return workflow task
     */
    public WorkflowTask getWorkflowTask(String taskKey) {
        UserTask userTask = userTasks.get(taskKey);
        if (userTask == null) {
            return null;
        }

        return WorkflowTask.builder()
                .key(userTask.getId())
                .name(userTask.getName())
                .actions(getActionsForTask(userTask))
                .build();
    }

    /**
     * Returns true if the current task is allowed for the current user.
     *
     * @param taskKey Key of task to check
     * @param authorizationHandler Authorization handler to use for role based permission checks
     * @param subject Subject to check for ole based based permission checks
     * @return true if the current user is allowed to access the task
     */
    public boolean isCurrentUserAllowed(
            final String taskKey,
            final AuthorizationHandler authorizationHandler,
            final Object subject) {
        final UserTask userTask = userTasks.get(taskKey);
        if (userTask == null) {
            return false;
        }

        final List<String> allowedUserTypes =
                findPropertyInHierarchy(userTask, ALLOWED_USER_TYPES_EXTENSION_PROPERTY)
                        .map(this::convertToList)
                        .orElse(DEFAULT_ALLOWED_USER_TYPES);
        final boolean passesAccessCheck =
                findPropertyInHierarchy(userTask, ALLOWED_ACTION_EXTENSION_PROPERTY)
                        .map(action -> authorizationHandler.isAllowedForInstance(action, subject))
                        .orElse(true);

        return allowedUserTypes.contains(
                        CurrentUserUtility.getCurrentUser()
                                .map(UserToken::getUserType)
                                .orElse("unknown"))
                && passesAccessCheck;
    }

    private List<WorkflowAction> getActionsForTask(UserTask task) {
        final List<WorkflowAction> actions =
                convertToList(getExtensionPropertyValue(task, ACTIONS_EXTENSION_PROPERTY)).stream()
                        .map(action -> configureWorkFlowActionFromUserTask(task, action))
                        .collect(Collectors.toList());

        // Apply default uiClass for actions that didn't directly name one.
        boolean isFirst = true;
        for (WorkflowAction action : actions) {
            if (action.getUiClass() == null) {
                if (isFirst) {
                    action.setUiClass(WorkflowAction.UI_CLASS_PRIMARY);
                } else {
                    action.setUiClass(WorkflowAction.UI_CLASS_SECONDARY);
                }
                isFirst = false;
            }
        }

        if (actions.isEmpty()) {
            actions.add(DEFAULT_ACTION);
        }

        return actions;
    }

    private WorkflowAction configureWorkFlowActionFromUserTask(UserTask task, String key) {
        final String uiLabel =
                getExtensionPropertyValue(
                        task, String.format(ACTION_LABEL_EXTENSION_PROPERTY, key));
        final String uiClass =
                getExtensionPropertyValue(
                        task, String.format(ACTION_CLASS_EXTENSION_PROPERTY, key));
        final String modalContext =
                getExtensionPropertyValue(
                        task, String.format(ACTION_MODAL_EXTENSION_PROPERTY, key));

        return WorkflowAction.builder()
                .key(key)
                .uiLabel(uiLabel)
                .uiClass(uiClass)
                .modalContext(modalContext)
                .build();
    }

    private String getExtensionPropertyValue(BaseElement element, String propertyName) {
        if (element.getExtensionElements() == null) {
            return null;
        }

        for (CamundaProperty property :
                element.getExtensionElements()
                        .getElementsQuery()
                        .filterByType(CamundaProperties.class)
                        .singleResult()
                        .getCamundaProperties()) {
            if (property.getCamundaName().equals(propertyName)) {
                return property.getCamundaValue();
            }
        }

        return null;
    }

    private Optional<String> findPropertyInHierarchy(BaseElement element, String propertyName) {
        final String propertyValue = getExtensionPropertyValue(element, propertyName);

        return Optional.ofNullable(propertyValue)
                .or(
                        () -> {
                            BaseElement parentElement = getParentElement(element);
                            if (parentElement != null) {
                                return findPropertyInHierarchy(parentElement, propertyName);
                            }
                            return Optional.empty();
                        });
    }

    private BaseElement getParentElement(BaseElement element) {
        // If element is in a swim lane get the lane (which is not the direct parent element in the
        // BPMN XML).
        if (element instanceof FlowNode) {
            for (Lane lane : modelInstance.getModelElementsByType(Lane.class)) {
                if (lane.getFlowNodeRefs().contains(element)) {
                    return lane;
                }
            }
        }

        return element.getParentElement() instanceof BaseElement
                ? (BaseElement) element.getParentElement()
                : null;
    }

    private List<String> convertToList(final String listAsString) {
        final List<String> values = new ArrayList<>();

        if (listAsString != null) {
            for (String value : listAsString.split(",")) {
                values.add(value.trim());
            }
        }
        return values;
    }
}
