package io.nuvalence.workmanager.service.utils.camunda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.token.UserToken;
import io.nuvalence.workmanager.service.domain.workflow.WorkflowTask;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class CamundaWorkflowInspectorTest {
    private CamundaWorkflowInspector inspector;
    @Mock private AuthorizationHandler authorizationHandler;

    @BeforeEach
    void setUp() {
        inspector =
                new CamundaWorkflowInspector(
                        Bpmn.readModelFromStream(
                                getClass().getResourceAsStream("/TestWorkflow.bpmn")));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getWorkflowTask() {
        final WorkflowTask task1 = inspector.getWorkflowTask("task1");
        final WorkflowTask task2 = inspector.getWorkflowTask("task2");

        assertEquals("task1", task1.getKey());
        assertEquals("Task 1", task1.getName());
        assertEquals("task2", task2.getKey());
    }

    @Test
    void workflowActionsCorrectlyDefaultUiClass() {
        final WorkflowTask task1 = inspector.getWorkflowTask("task1");
        final WorkflowTask task2 = inspector.getWorkflowTask("task2");

        assertEquals("Secondary", task1.getActions().get(0).getUiClass());
        assertEquals("Primary", task2.getActions().get(0).getUiClass());
        assertEquals("Secondary", task2.getActions().get(1).getUiClass());
    }

    @Test
    void tasksWillIncludeDefaultActionsWhenNoActionsConfigured() {
        final WorkflowTask task3 = inspector.getWorkflowTask("task3");

        assertEquals(1, task3.getActions().size());
        assertEquals("Submit", task3.getActions().get(0).getKey());
        assertEquals("Submit", task3.getActions().get(0).getUiLabel());
        assertEquals("Primary", task3.getActions().get(0).getUiClass());
    }

    @Test
    void isCurrentUserAllowedWillReturnTrueWhenUserTypeInAllowedList() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UserToken.builder()
                                .userType("agency")
                                .roles(Collections.emptyList())
                                .build());

        assertTrue(inspector.isCurrentUserAllowed("task2", authorizationHandler, null));
    }

    @Test
    void isCurrentUserAllowedWillReturnFalseWhenUserTypeNotInAllowedList() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UserToken.builder()
                                .userType("public")
                                .roles(Collections.emptyList())
                                .build());

        assertFalse(inspector.isCurrentUserAllowed("task2", authorizationHandler, null));
    }
}
