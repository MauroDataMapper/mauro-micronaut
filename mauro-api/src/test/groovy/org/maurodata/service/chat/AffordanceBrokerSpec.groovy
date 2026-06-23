package org.maurodata.service.chat

import org.maurodata.api.chat.AffordanceDto
import spock.lang.Specification

class AffordanceBrokerSpec extends Specification {

    void 'derives mauro_get affordance from DataModel artefact state'() {
        given:
        AffordanceBroker broker = new AffordanceBroker()

        when:
        List<AffordanceDto> affordances = broker.derive(new AffordanceContext(
            sourceType: 'tool_result',
            sourceName: 'mauro_keyword_search',
            artefacts: [
                [domainType: 'DataModel', id: 'dm-1', label: 'Braden Risk Assessment'],
                [domainType: 'DataElement', id: 'de-1', label: 'Score']
            ] as List<Object>
        ))

        then:
        affordances.size() == 1
        affordances[0].kind == 'tool_call'
        affordances[0].target.name == 'mauro_get'
        affordances[0].arguments.uri == 'mauro-api://http-get/api/dataModels/dm-1'
        affordances[0].artefact.type == 'DataModel'
        affordances[0].artefact.ordinal == '1'
        affordances[0].title == 'Read result 1 DataModel Braden Risk Assessment'
        affordances[0].sourceType == 'tool_result'
        affordances[0].sourceName == 'mauro_keyword_search'
    }

    void 'renders model actions from structured affordance maps'() {
        given:
        AffordanceBroker broker = new AffordanceBroker()
        List<Map<String, Object>> affordances = broker.deriveMaps(new AffordanceContext(
            sourceType: 'tool_result',
            sourceName: 'mauro_keyword_search',
            result: [
                items: [
                    [domainType: 'DataModel', id: 'dm-1', label: 'Braden Risk Assessment']
                ]
            ] as Map<String, Object>
        ))

        when:
        List<String> rendered = broker.renderModelActions(affordances)

        then:
        affordances[0].name == 'mauro_get'
        rendered.join('\n').contains('Read result 1 DataModel Braden Risk Assessment')
        rendered.join('\n').contains('"name":"mauro_get"')
        rendered.join('\n').contains('mauro-api://http-get/api/dataModels/dm-1')
        rendered.join('\n').contains('CW: Review the current user request as a checklist of requested actions')
        rendered.join('\n').contains('CW: The current user request is sufficient confirmation')
        rendered.join('\n').contains('execute that exact action in this turn')
        rendered.join('\n').contains('as progress through work you are doing for the user')
        rendered.last().contains('FR: Mention optional follow-up actions')
        rendered.join('\n').indexOf('CW: Read result 1 DataModel Braden Risk Assessment') < rendered.join('\n').indexOf('CW: If an exact action above completes the next unfinished requested action')
    }
}
