package org.maurodata.service.chat.mcp

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class FailedHttpResourceInterpretation implements ResultInterpretation {

    @Override
    String id() {
        'failed-http-resource'
    }

    @Override
    Integer priority() {
        10
    }

    @Override
    List<String> appliesToResourceNames() {
        []
    }

    @Override
    List<String> appliesToTypes() {
        ['HttpResourceResult']
    }

    @Override
    boolean supports(ResultContext context) {
        context?.statusCode != null && !context.successfulHttpStatus()
    }

    @Override
    ResultInterpretationOutput interpret(ResultContext context) {
        List<String> answerInstructions = [
            "COMMON: The resource read returned HTTP ${context.statusCode}; do not interpret the returned body as successful resource content.".toString()
        ] as List<String>
        if (context.statusCode == 404) {
            answerInstructions.add('IC: When the current user request or recent conversation contains a catalogue label/form name/model name, actively verify the exact label-to-ID pairing.')
            answerInstructions.add('IC: If the URI id does not match the requested label, call the correct mauro_get action or call mauro_search for the exact label and retry.')
            answerInstructions.add('IC: Do not ask the user to repeat a label that is already present in the conversation.')
            answerInstructions.add('FF: Give a final not-found answer only after IC has been attempted, or when no requested label/name/reference exists in the conversation.')
        } else {
            answerInstructions.add("FF: Tell the user the resource could not be read and include HTTP status ${context.statusCode}.".toString())
        }
        new ResultInterpretationOutput(
            id: id(),
            title: 'Resource Read Failed',
            statements: [
                'The resource could not be read.',
                "The backend HTTP status is ${context.statusCode}, so this resource read did not succeed.".toString()
            ],
            answerInstructions: answerInstructions
        )
    }
}
