package org.maurodata.service.overlay

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.Path
import org.maurodata.domain.overlay.OverlayAction
import org.maurodata.domain.model.ModelPathMatch
import org.maurodata.domain.overlay.OverlayRule

/** Projects shared model-item descriptions and writing metadata into generic Overlay rules. */
@CompileStatic
@Singleton
class DataModelOverlayGeneratorService {
    static final String SETTINGS_NAMESPACE = 'org.maurodata.overlay'

    List<OverlayRule> generate(DataModel model) {
        boolean matchPathSuffix = setting(model, 'matchPathSuffix', true)
        boolean firstIdentifierSuffix = setting(model, 'matchFirstNodeIdentifierSuffix', true)
        boolean allowMultipleModels = setting(model, 'allowMultipleModels', true)
        List<OverlayRule> rules = []
        model.dataClasses?.each { DataClass dataClass ->
            addClassRules(dataClass, [], rules, matchPathSuffix, firstIdentifierSuffix, allowMultipleModels)
        }
        rules
    }

    private void addClassRules(DataClass dataClass, List<String> ancestors, List<OverlayRule> rules,
                               boolean matchPathSuffix, boolean firstIdentifierSuffix, boolean allowMultipleModels) {
        List<String> classPath = new ArrayList<String>(ancestors)
        classPath.add(dataClass.label)
        List<OverlayAction> classActions = actionsFor(dataClass)
        if (classActions) rules.add(rule(classPath, null, classActions, matchPathSuffix, firstIdentifierSuffix, allowMultipleModels))

        dataClass.dataElements?.each { DataElement dataElement ->
            List<OverlayAction> elementActions = actionsFor(dataElement)
            if (elementActions) rules.add(rule(classPath, dataElement.label, elementActions,
                matchPathSuffix, firstIdentifierSuffix, allowMultipleModels))
        }
        dataClass.dataClasses?.each { DataClass child ->
            addClassRules(child, classPath, rules, matchPathSuffix, firstIdentifierSuffix, allowMultipleModels)
        }
    }

    private static OverlayRule rule(List<String> classPath, String elementLabel, List<OverlayAction> actions,
                                    boolean matchPathSuffix, boolean firstIdentifierSuffix,
                                    boolean allowMultipleModels) {
        List<Path.PathNode> nodes = classPath.collect { String label -> new Path.PathNode(prefix: 'dc', identifier: label) }
        if (elementLabel != null) nodes.add(new Path.PathNode(prefix: 'de', identifier: elementLabel))
        new OverlayRule(match: new ModelPathMatch(path: new Path(nodes), matchPathSuffix: matchPathSuffix,
            matchFirstNodeIdentifierSuffix: firstIdentifierSuffix, allowMultipleModels: allowMultipleModels), actions: actions)
    }

    private static List<OverlayAction> actionsFor(org.maurodata.domain.model.AdministeredItem item) {
        List<OverlayAction> actions = []
        if (item.description?.trim()) {
            actions.add(new OverlayAction(type: 'value', field: 'description', value: item.description, policy: 'always'))
        }
        item.metadata?.each { Metadata metadata ->
            if (metadata.namespace && metadata.key && metadata.value != null) {
                actions.add(new OverlayAction(type: 'metadata', namespace: metadata.namespace, key: metadata.key,
                    value: metadata.value, policy: 'always'))
            }
        }
        item.annotations?.each { Annotation annotation ->
            if (annotation.label?.trim() && annotation.description != null) {
                actions.add(new OverlayAction(type: 'annotation', label: annotation.label,
                    description: annotation.description, policy: 'always'))
            }
        }
        actions
    }

    private static boolean setting(DataModel model, String key, boolean defaultValue) {
        Metadata setting = model.metadata?.find { Metadata item -> item.namespace == SETTINGS_NAMESPACE && item.key == key }
        if (setting == null || !setting.value?.trim()) return defaultValue
        setting.value.equalsIgnoreCase('true')
    }
}
