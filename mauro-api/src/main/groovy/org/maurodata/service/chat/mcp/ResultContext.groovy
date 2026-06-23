package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic

@CompileStatic
class ResultContext {

    String sourceName
    String resourceName
    String uri
    String mimeType
    Integer statusCode
    Map<String, Object> result = [:]
    List<Object> artefacts = []

    boolean successfulHttpStatus() {
        statusCode != null && statusCode >= 200 && statusCode < 300
    }

    List<Map<String, Object>> artefactMaps() {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>()
        for (Object artefact : artefacts ?: []) {
            if (artefact instanceof Map) {
                @SuppressWarnings('unchecked')
                Map<String, Object> map = (Map<String, Object>) artefact
                maps.add(map)
            }
        }
        maps
    }

    List<Map<String, Object>> artefactsOfType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return Collections.<Map<String, Object>>emptyList()
        }
        List<Map<String, Object>> matches = new ArrayList<Map<String, Object>>()
        for (Map<String, Object> artefact : artefactMaps()) {
            String artefactType = asString(artefact.get('type'))
            String domainType = asString(artefact.get('domainType'))
            if (type == artefactType || type == domainType) {
                matches.add(artefact)
            }
        }
        matches
    }

    private static String asString(Object value) {
        value == null ? null : String.valueOf(value)
    }
}
