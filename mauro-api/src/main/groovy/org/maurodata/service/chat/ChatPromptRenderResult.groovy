package org.maurodata.service.chat

import groovy.transform.CompileStatic

@CompileStatic
class ChatPromptRenderResult {
    String text
    String assetId
    String assetVersion
    String assetType
    Boolean fallbackUsed = false
    List<Map<String, Object>> fragments = []
    List<String> variableNames = []
    List<String> redactedVariableNames = []

    Map<String, Object> toMetadata() {
        [
            assetId              : assetId,
            assetVersion         : assetVersion,
            assetType            : assetType,
            fallbackUsed         : Boolean.TRUE == fallbackUsed,
            fragments            : fragments,
            variableNames        : variableNames,
            redactedVariableNames: redactedVariableNames
        ] as Map<String, Object>
    }
}
