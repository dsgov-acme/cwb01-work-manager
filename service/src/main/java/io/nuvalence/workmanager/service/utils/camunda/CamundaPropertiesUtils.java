package io.nuvalence.workmanager.service.utils.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utils for handling Camunda properties.
 */
public class CamundaPropertiesUtils {

    private CamundaPropertiesUtils() {
        throw new AssertionError(
                "Utility class should not be instantiated, use the static methods.");
    }

    /**
     * Gets a Camunda extension property.
     *
     * @param propertyName name of the desired property.
     * @param execution the delegate execution.
     * @return an optional of the value of the property.
     */
    public static Optional<String> getExtensionProperty(
            String propertyName, DelegateExecution execution) {
        CamundaProperties camundaProperties =
                execution
                        .getBpmnModelElementInstance()
                        .getExtensionElements()
                        .getElementsQuery()
                        .filterByType(CamundaProperties.class)
                        .singleResult();

        if (camundaProperties != null) {
            return camundaProperties.getCamundaProperties().stream()
                    .filter(property -> property.getCamundaName().equals(propertyName))
                    .map(CamundaProperty::getCamundaValue)
                    .findFirst();
        }

        return Optional.empty();
    }

    /**
     * Gets Camunda properties with a given prefix.
     *
     * @param prefix Prefix if the properties' names.
     * @param execution Camunda execution.
     *
     * @return Map of found variables.
     */
    public static Map<String, String> getExtensionPropertiesWithPrefix(
            String prefix, DelegateExecution execution) {
        Map<String, String> propertiesWithPrefix = new HashMap<>();

        CamundaProperties camundaProperties =
                execution
                        .getBpmnModelElementInstance()
                        .getExtensionElements()
                        .getElementsQuery()
                        .filterByType(CamundaProperties.class)
                        .singleResult();

        if (camundaProperties != null) {
            camundaProperties.getCamundaProperties().stream()
                    .filter(property -> property.getCamundaName().startsWith(prefix))
                    .forEach(
                            propertyWithPrefix -> {
                                propertiesWithPrefix.put(
                                        propertyWithPrefix
                                                .getCamundaName()
                                                .substring(prefix.length()),
                                        propertyWithPrefix.getCamundaValue());
                            });
        }

        return propertiesWithPrefix;
    }
}
