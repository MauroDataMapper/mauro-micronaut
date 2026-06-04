package org.maurodata.service.chat

import groovy.transform.CompileStatic

@CompileStatic
class ChatSkillDefinition {
    String id
    String name
    String description
    String scope
    String version
    String type
    Integer priority
    List<String> keywords = []
    SkillRouting routing = new SkillRouting()
    String instruction
}

@CompileStatic
class SkillRouting {
    String specificity = 'NORMAL'
    List<String> useWhen = []
    List<String> avoidWhen = []
    List<String> examples = []
    String toolName
    Map<String, Object> toolArguments = [:]
}
