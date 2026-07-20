package org.maurodata.service.chat

import groovy.transform.CompileStatic

@CompileStatic
class AffordanceContext {
    String sourceType
    String sourceName
    String sourceId
    String text

    Map<String, Object> result = [:]
    List<Object> artefacts = []

    List<Map<String, Object>> availableTools = []
    List<Map<String, Object>> availableResources = []
    List<ChatPromptAssetDefinition> availableSkills = []
}
