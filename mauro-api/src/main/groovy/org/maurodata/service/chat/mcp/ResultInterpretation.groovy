package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic

@CompileStatic
interface ResultInterpretation {

    String id()

    Integer priority()

    List<String> appliesToResourceNames()

    List<String> appliesToTypes()

    boolean supports(ResultContext context)

    ResultInterpretationOutput interpret(ResultContext context)
}
