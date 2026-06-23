package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class AffordanceDto {
    String id
    String kind
    String title
    String description

    String sourceType
    String sourceName
    String sourceId

    Map<String, Object> target = [:]
    Map<String, Object> arguments = [:]
    Map<String, Object> artefact = [:]

    Boolean modelVisible = true
    Boolean uiVisible = true
    Integer priority = 100

    Map<String, Object> toMap() {
        Map<String, Object> out = new LinkedHashMap<String, Object>()
        putIfPresent(out, 'id', id)
        putIfPresent(out, 'kind', kind)
        putIfPresent(out, 'title', title)
        putIfPresent(out, 'description', description)
        putIfPresent(out, 'sourceType', sourceType)
        putIfPresent(out, 'sourceName', sourceName)
        putIfPresent(out, 'sourceId', sourceId)
        out.put('target', target ?: Collections.<String, Object>emptyMap())
        if (target != null && target.get('name') != null) {
            out.put('name', target.get('name'))
        }
        out.put('arguments', arguments ?: Collections.<String, Object>emptyMap())
        out.put('artefact', artefact ?: Collections.<String, Object>emptyMap())
        out.put('modelVisible', modelVisible)
        out.put('uiVisible', uiVisible)
        out.put('priority', priority)
        out
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            target.put(key, value)
        }
    }
}
