package org.maurodata.service.chat.llm.config

import groovy.transform.CompileStatic

@CompileStatic
class ChatProviderConfiguration {
    String id
    String type
    String baseUrl
    String apiKey
    List<String> modelAllowlist = []
    Integer timeoutSeconds
    Boolean logRequests
    Boolean logResponses
    Map<String, Object> defaultParameters = [:]

    boolean hasApiKey() {
        apiKey != null && !apiKey.trim().isEmpty()
    }

    String effectiveType() {
        type == null || type.trim().isEmpty() ? id : type
    }
}
