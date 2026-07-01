package org.maurodata.service.chat.mcp

import org.maurodata.plugin.chat.mcp.DataModelResourceInterpretation
import org.maurodata.service.chat.AffordanceBroker
import spock.lang.Specification

class ResultGuidanceServiceSpec extends Specification {

    void 'adds available next actions for returned DataModel artefacts'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()
        Map<String, Object> result = [
            withGuidance: true,
            items       : [
                [domainType: 'DataModel', id: 'dm-1', label: 'Braden Risk Assessment'],
                [domainType: 'DataElement', id: 'de-1', label: 'Score']
            ]
        ] as Map<String, Object>

        when:
        String text = service.applyToolGuidance('mauro_keyword_search', result, '## Returned Data\n1. Braden Risk Assessment')

        then:
        text.contains('## Available Next Actions')
        text.contains('returned artefacts')
        text.contains('Braden Risk Assessment')
        text.contains('"name":"mauro_get"')
        text.contains('mauro-api://http-get/api/dataModels/dm-1')
        text.contains('Do not invent endpoint URIs or call skill ids as tools')
        text.contains('CW: Read result 1 DataModel Braden Risk Assessment')
        result.affordances instanceof List
        result.affordances.size() == 1
        result.affordances[0].kind == 'tool_call'
        result.affordances[0].name == 'mauro_get'
        result.affordances[0].arguments.uri == 'mauro-api://http-get/api/dataModels/dm-1'
    }

    void 'inserts available next actions before END section when present'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()
        Map<String, Object> result = [
            withGuidance: true,
            items       : [
                [domainType: 'DataModel', id: 'dm-1', label: 'Braden Risk Assessment']
            ]
        ] as Map<String, Object>

        when:
        String text = service.applyToolGuidance('mauro_keyword_search', result, '## Answer Instructions\nCOMMON: show results\n\n## END\nEND: final reminder')

        then:
        text.indexOf('## Available Next Actions') > text.indexOf('## Answer Instructions')
        text.indexOf('## Available Next Actions') < text.indexOf('## END')
    }

    void 'does not add guidance when withGuidance is false'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()

        when:
        String text = service.applyToolGuidance('mauro_keyword_search', [
            withGuidance: false,
            items       : [
                [domainType: 'DataModel', id: 'dm-1', label: 'Braden Risk Assessment']
            ]
        ] as Map<String, Object>, 'base text')

        then:
        text == 'base text'
    }

    void 'limits DataModel actions to avoid flooding the model'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()

        when:
        String text = service.applyToolGuidance('mauro_keyword_search', [
            items: [
                [domainType: 'DataModel', id: 'dm-1', label: 'One'],
                [domainType: 'DataModel', id: 'dm-2', label: 'Two'],
                [domainType: 'DataModel', id: 'dm-3', label: 'Three'],
                [domainType: 'DataModel', id: 'dm-4', label: 'Four']
            ]
        ] as Map<String, Object>, 'base text')

        then:
        text.contains('dataModels/dm-1')
        text.contains('dataModels/dm-2')
        text.contains('dataModels/dm-3')
        !text.contains('dataModels/dm-4')
    }

    void 'adds resource read interpretation for successful DataModel resources'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()

        when:
        String text = service.applyToolGuidance('mauro_get', [
            withGuidance: true,
            statusCode  : 200,
            name        : 'DataModel.show',
            id          : 'dm-1',
            label       : 'Braden Risk Assessment',
            domainType  : 'DataModel',
            data        : [
                id         : 'dm-1',
                label      : 'Braden Risk Assessment',
                domainType : 'DataModel',
                path       : 'fo:Forms|dm:Braden Risk Assessment$main',
                classifiers: [
                    [label: 'Cerner Millennium']
                ],
                metadata   : [
                    [namespace: 'org.maurodata.cerner.powerforms', key: 'DESCRIPTION', value: 'Braden Risk Assessment']
                ],
                finalised  : false,
                type       : 'Data Asset'
            ]
        ] as Map<String, Object>, 'base text')

        then:
        text.contains('## Additional Interpretations')
        !text.contains('authoritative structured Mauro API representation of one DataModel')
        text.contains('not as a search result page')
        text.contains('Useful fields to consider when summarising the raw DataModel JSON')
        !text.contains('Optional distilled data')
        text.contains('- domainType: DataModel')
        text.contains('- classifiers: Cerner Millennium')
        text.contains('- metadata: org.maurodata.cerner.powerforms.DESCRIPTION=Braden Risk Assessment')
        text.contains('escape any pipe characters inside cell values as \\|')
        text.contains('prefer putting that value in a bullet or fenced code block outside the table')
        text.contains('Answer instructions from this interpretation:')
    }

    void 'adds resource read failure interpretation from backend status'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()

        when:
        String text = service.applyToolGuidance('mauro_get', [
            withGuidance: true,
            statusCode  : 404,
            name        : 'DataModel.show'
        ] as Map<String, Object>, 'base text')

        then:
        text.contains('## Additional Interpretations')
        text.contains('HTTP status is 404')
        text.contains('could not be read')
        text.toLowerCase(Locale.ROOT).contains('do not interpret the returned body as successful resource content')
    }

    void 'executable DataModel interpretation matches by resource name and distils identity fields'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()

        when:
        List<ResultInterpretationOutput> outputs = service.interpret(new ResultContext(
            sourceName: 'mauro_get',
            resourceName: 'DataModel.show',
            statusCode: 200,
            result: [
                id        : 'dm-1',
                label     : 'Braden Risk Assessment',
                domainType: 'DataModel',
                classifiers: [
                    [label: 'Cerner Millennium']
                ],
                metadata  : [
                    [namespace: 'org.maurodata.cerner.powerforms', key: 'DESCRIPTION', value: 'Braden Risk Assessment']
                ],
                finalised : false,
                type      : 'Data Asset'
            ] as Map<String, Object>
        ))

        then:
        outputs*.id.contains('data-model-resource')
        ResultInterpretationOutput output = outputs.find {ResultInterpretationOutput item -> item.id == 'data-model-resource'}
        output.distilledData.id == 'dm-1'
        output.distilledData.label == 'Braden Risk Assessment'
        output.distilledData.classifiers == ['Cerner Millennium']
        output.distilledData.metadata == ['org.maurodata.cerner.powerforms.DESCRIPTION=Braden Risk Assessment']
        output.distilledData.finalised == false
        output.renderedContent.contains('Useful fields to consider when summarising the raw DataModel JSON:')
        output.renderedContent.any {String item -> item.contains('- label: Braden Risk Assessment') }
        output.answerInstructions.any {String item -> item.contains('Do not invent child classes') }
    }

    void 'DataModel interpretation does not treat resource route description as model description'() {
        given:
        ResultGuidanceService service = guidanceServiceWithDataModelInterpretation()

        when:
        String text = service.applyToolGuidance('mauro_get', [
            withGuidance       : true,
            statusCode         : 200,
            name               : 'DataModel.show',
            id                 : 'dm-1',
            label              : 'Braden Risk Assessment',
            domainType         : 'DataModel',
            resourceDescription: 'HTTP GET /api/dataModels/{id} (DataModel.show)',
            data               : [
                id        : 'dm-1',
                label     : 'Braden Risk Assessment',
                domainType: 'DataModel',
                description: 'Clinical pressure ulcer risk assessment form',
                path      : 'fo:Forms|dm:Braden Risk Assessment$main'
            ]
        ] as Map<String, Object>, 'base text')

        then:
        text.contains('- label: Braden Risk Assessment')
        text.contains('- description: Clinical pressure ulcer risk assessment form')
        text.contains('- path: fo:Forms|dm:Braden Risk Assessment$main')
        !text.contains('| description | HTTP GET /api/dataModels/{id} (DataModel.show) |')
        !text.contains('- description: HTTP GET /api/dataModels/{id} (DataModel.show)')
    }

    private static ResultGuidanceService guidanceServiceWithDataModelInterpretation() {
        new ResultGuidanceService(
            new AffordanceBroker(),
            [
                new FailedHttpResourceInterpretation(),
                new DataModelResourceInterpretation()
            ] as List<ResultInterpretation>
        )
    }
}
