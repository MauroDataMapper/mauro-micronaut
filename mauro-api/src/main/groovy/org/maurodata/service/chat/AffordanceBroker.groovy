package org.maurodata.service.chat

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.AffordanceDto
import org.maurodata.service.chat.mcp.McpHttpResourceRegistry

@CompileStatic
@Singleton
class AffordanceBroker {

    private static final int MAX_ACTIONS_PER_TYPE = 3
    private static final String DATA_MODEL_SHOW_FALLBACK = McpHttpResourceRegistry.URI_PREFIX + '/api/dataModels/{id}'

    private final McpHttpResourceRegistry resourceRegistry

    @Inject
    AffordanceBroker(McpHttpResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry
    }

    AffordanceBroker() {
        this(null)
    }

    List<AffordanceDto> derive(AffordanceContext context) {
        if (context == null) {
            return Collections.<AffordanceDto>emptyList()
        }
        List<AffordanceDto> affordances = new ArrayList<AffordanceDto>()
        affordances.addAll(dataModelResourceReadAffordances(context, extractDataModelArtefacts(context)))
        affordances.sort {AffordanceDto left, AffordanceDto right ->
            Integer.valueOf(left.priority ?: 100) <=> Integer.valueOf(right.priority ?: 100)
        }
        affordances
    }

    List<Map<String, Object>> deriveMaps(AffordanceContext context) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>()
        for (AffordanceDto affordance : derive(context)) {
            out.add(affordance.toMap())
        }
        out
    }

    List<String> renderModelActions(List<Map<String, Object>> affordances) {
        if (affordances == null || affordances.isEmpty()) {
            return Collections.<String>emptyList()
        }
        List<String> actions = new ArrayList<String>()
        actions.add('CW: These next actions are derived from returned artefacts and currently available tools/resources.')
        actions.add('CW: Review the current user request as a checklist of requested actions and identify any requested action that is not complete yet.')
        actions.add('CW: Use these exact actions when they match the remaining user request. Do not invent endpoint URIs or call skill ids as tools.')
        actions.add('CW: If the user names a returned item, match the exact returned label to its own ID before calling the action. Do not copy an ID from a different returned label.')
        actions.add('CW: The current user request is sufficient confirmation for any exact action that completes one of its unfinished requested actions.')
        for (Map<String, Object> affordance : affordances) {
            if (Boolean.FALSE.equals(affordance.get('modelVisible'))) {
                continue
            }
            String title = asString(affordance.get('title')) ?: asString(affordance.get('name')) ?: asString(affordance.get('kind')) ?: 'Available action'
            Map<String, Object> call = [
                name     : toolName(affordance),
                arguments: affordance.get('arguments') ?: Collections.<String, Object>emptyMap()
            ] as Map<String, Object>
            actions.add("CW: ${title}: ${JsonOutput.toJson(call)}".toString())
        }
        actions.add('CW: After presenting any required current result, compare the requested-action checklist with the exact actions above.')
        actions.add('CW: If an exact action above completes the next unfinished requested action, execute that exact action in this turn.')
        actions.add('CW: Present that exact action as progress through work you are doing for the user.')
        actions.add('FR: Mention optional follow-up actions such as next page or refined search only after requested actions are complete, or when no exact action above can complete the unfinished requested action.')
        actions
    }

    private List<AffordanceDto> dataModelResourceReadAffordances(AffordanceContext context, List<Map<String, String>> dataModels) {
        if (dataModels.isEmpty()) {
            return Collections.<AffordanceDto>emptyList()
        }
        String template = dataModelShowUriTemplate()
        if (template == null || template.trim().isEmpty()) {
            return Collections.<AffordanceDto>emptyList()
        }

        List<AffordanceDto> affordances = new ArrayList<AffordanceDto>()
        int limit = Math.min(dataModels.size(), MAX_ACTIONS_PER_TYPE)
        for (int i = 0; i < limit; i++) {
            Map<String, String> dataModel = dataModels.get(i)
            String id = dataModel.id
            String label = dataModel.label ?: id
            String resultOrdinal = dataModel.get('ordinal') ?: String.valueOf(i + 1)
            String uri = template.replace('{id}', id)
            affordances.add(new AffordanceDto(
                id: "mauro_get:DataModel:${id}".toString(),
                kind: 'tool_call',
                title: "Read result ${resultOrdinal} DataModel ${label}".toString(),
                description: "Read the authoritative structured Mauro API representation for result ${resultOrdinal} (${label}).".toString(),
                sourceType: context.sourceType,
                sourceName: context.sourceName,
                sourceId: context.sourceId,
                target: [
                    name: 'mauro_get'
                ] as Map<String, Object>,
                arguments: [
                    uri: uri
                ] as Map<String, Object>,
                artefact: [
                    type      : 'DataModel',
                    id        : id,
                    label     : label,
                    domainType: 'DataModel',
                    ordinal   : resultOrdinal
                ] as Map<String, Object>,
                modelVisible: true,
                uiVisible: true,
                priority: Integer.valueOf(10 + i)
            ))
        }
        affordances
    }

    private static List<Map<String, String>> extractDataModelArtefacts(AffordanceContext context) {
        List<Map<String, String>> artefacts = new ArrayList<Map<String, String>>()
        for (Object artefactObj : context.artefacts ?: []) {
            if (artefactObj instanceof Map) {
                addDataModelArtefact(artefacts, (Map<?, ?>) artefactObj, null)
            }
        }
        Object rawItems = context.result == null ? null : context.result.get('items')
        if (rawItems instanceof Collection) {
            int index = 1
            for (Object itemObj : (Collection<?>) rawItems) {
                if (itemObj instanceof Map) {
                    addDataModelArtefact(artefacts, (Map<?, ?>) itemObj, String.valueOf(index))
                }
                index++
            }
        }
        artefacts
    }

    private static void addDataModelArtefact(List<Map<String, String>> artefacts, Map<?, ?> item, String ordinal) {
        String domainType = asString(item.get('domainType'))
        String type = asString(item.get('type'))
        String id = asString(item.get('id'))
        if ((domainType == 'DataModel' || type == 'DataModel') && id != null && !id.trim().isEmpty()) {
            Map<String, String> existing = artefacts.find {Map<String, String> candidate -> candidate.id == id}
            if (existing == null) {
                artefacts.add([
                    id     : id,
                    label  : asString(item.get('label')) ?: id,
                    ordinal: ordinal
                ] as Map<String, String>)
            } else if ((existing.get('ordinal') == null || existing.get('ordinal').trim().isEmpty()) && ordinal != null && !ordinal.trim().isEmpty()) {
                existing.put('ordinal', ordinal)
            }
        }
    }

    private String dataModelShowUriTemplate() {
        if (resourceRegistry != null) {
            McpHttpResourceRegistry.McpHttpResource resource = resourceRegistry.listResourceTemplates()
                .find {McpHttpResourceRegistry.McpHttpResource candidate ->
                    candidate.name == 'DataModel.show' || candidate.path == '/api/dataModels/{id}'
                }
            if (resource != null && resource.uriTemplate != null && !resource.uriTemplate.trim().isEmpty()) {
                return resource.uriTemplate
            }
        }
        DATA_MODEL_SHOW_FALLBACK
    }

    private static String toolName(Map<String, Object> affordance) {
        Object targetObj = affordance.get('target')
        if (targetObj instanceof Map) {
            Object name = ((Map<?, ?>) targetObj).get('name')
            if (name != null && !String.valueOf(name).trim().isEmpty()) {
                return String.valueOf(name)
            }
        }
        asString(affordance.get('name')) ?: ''
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }
}
