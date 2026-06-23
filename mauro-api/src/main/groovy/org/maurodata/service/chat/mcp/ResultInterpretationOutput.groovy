package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic

@CompileStatic
class ResultInterpretationOutput {

    String id
    String title
    List<String> statements = []
    List<String> renderedContent = []
    List<String> answerInstructions = []
    Map<String, Object> distilledData = [:]
}
