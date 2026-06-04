package org.maurodata.service.chat.llm

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@CompileStatic
@Canonical
class ProviderRequest {
    String sessionId
    String messageId
    String model
    List<ProviderMessage> messages = []
    List<Map<String, Object>> tools = []
    Map<String, Object> options = [:]
}

@CompileStatic
@Canonical
class ProviderMessage {
    String role
    String content
    String toolCallId
    String name
    List<Map<String, Object>> toolCalls = []
}
