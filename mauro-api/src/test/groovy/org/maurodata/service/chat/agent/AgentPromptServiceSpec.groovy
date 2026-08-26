package org.maurodata.service.chat.agent

import org.maurodata.service.chat.ChatPromptComposer
import spock.lang.Specification

import java.time.Instant

class AgentPromptServiceSpec extends Specification {

    void 'planner and plan evaluator receive compact evidence while step evaluator sees current payload'() {
        given:
        AgentPromptService service = new AgentPromptService(new ChatPromptComposer(null))
        AgentRunRecord run = run()
        AgentPlanRecord plan = plan()
        AgentContextRecord context = context()
        AgentStepRecord step = toolStep()
        AgentEvidenceRecord evidence = searchEvidence()
        String largeEvidenceBody = evidence.content
        AgentEvidenceRecord readEvidence = readEvidence()

        when:
        String plannerPrompt = service.plannerUserPrompt(run, tools(), [evidence], null, context)
        String stepEvaluatorPrompt = service.stepEvaluatorUserPrompt(run, plan, context, step, [evidence, readEvidence], [readEvidence], [])
        String planEvaluatorPrompt = service.planEvaluatorUserPrompt(run, plan, context, step, [evidence], [])

        then:
        !plannerPrompt.contains(largeEvidenceBody)
        !planEvaluatorPrompt.contains(largeEvidenceBody)
        plannerPrompt.contains('Adult Diabetes Eye Assessment Form')
        plannerPrompt.contains('mauro-api://http-get/api/dataModels/adult-eye')
        plannerPrompt.contains('"count":"2"') || plannerPrompt.contains('"count":2')
        plannerPrompt.contains('view: compact projection; not a complete payload')
        plannerPrompt.contains('omitted fields are not evidence of absence')
        stepEvaluatorPrompt.contains('"dataClasses"')
        stepEvaluatorPrompt.contains('Clinical details')
        stepEvaluatorPrompt.contains('HbA1c')
        stepEvaluatorPrompt.contains('payload view: chars')
        planEvaluatorPrompt.contains('Adult Diabetes Eye Assessment Form')
    }

    void 'executor receives full prior evidence only for no-tool analysis style steps'() {
        given:
        AgentPromptService service = new AgentPromptService(new ChatPromptComposer(null))
        AgentRunRecord run = run()
        AgentPlanRecord plan = plan()
        AgentContextRecord context = context()
        AgentEvidenceRecord evidence = searchEvidence()

        when:
        String toolStepPrompt = service.executorUserPrompt(run, plan, context, toolStep(), [evidence], [])
        String analysisStepPrompt = service.executorUserPrompt(run, plan, context, analysisStep(), [evidence], [])

        then:
        !toolStepPrompt.contains(evidence.content)
        toolStepPrompt.contains('Adult Diabetes Eye Assessment Form')
        analysisStepPrompt.contains(evidence.content)
    }

    void 'mauro_get evidence receives a larger render budget for inspection steps'() {
        given:
        AgentPromptService service = new AgentPromptService(new ChatPromptComposer(null))
        AgentRunRecord run = run()
        AgentPlanRecord plan = plan()
        AgentContextRecord context = context()
        AgentStepRecord step = analysisStep()
        AgentEvidenceRecord readEvidence = readEvidenceWithLateStructure()

        when:
        String analysisPrompt = service.executorUserPrompt(run, plan, context, step, [readEvidence], [])
        String evaluatorPrompt = service.stepEvaluatorUserPrompt(run, plan, context, step, [readEvidence], [readEvidence], [])
        String finalPrompt = service.finalWriterUserPrompt(run, plan, context, [readEvidence], [])

        then:
        analysisPrompt.contains('Late section marker')
        analysisPrompt.contains('Late field marker')
        evaluatorPrompt.contains('Late section marker')
        evaluatorPrompt.contains('Late field marker')
        finalPrompt.contains('Late section marker')
        finalPrompt.contains('Late field marker')
        finalPrompt.contains('Evidence payload view: chars')
        finalPrompt.contains('complete=true')
    }

    void 'final writer context excludes operational context and tool guidance'() {
        given:
        AgentPromptService service = new AgentPromptService(new ChatPromptComposer(null))
        AgentContextRecord context = context()
        context.metadata = [
            personaGuidance: '''## Role
- You are an assistant inside Mauro Data Mapper.

## Context
- The user may know Mauro concepts better than you do.

## Behaviour
- Do not invent facts about Mauro or the connected catalogue.
- Be direct, practical, and helpful.

## Style
- Answer in clear language.
- Be concise unless the user asks for detail.''',
            skillLookupGuidance: 'Use mauro_get and mauro_describe to retrieve missing DataClass/DataElement routes before answering.'
        ] as Map<String, Object>
        context.recommendedSkills = [[id: 'mauro-data-model-explorer', usage: 'retrieve structure before answering'] as Map<String, Object>]
        context.contextRequests = [[type: 'session_resource_lookup', reason: 'find more routes'] as Map<String, Object>]
        context.planningHints = ['Call mauro_describe before final answer.']
        context.instructions = [
            [target: 'executor', instruction: 'Call mauro_get with guidance.'] as Map<String, Object>,
            [target: 'final_writer', instruction: 'Mention limitations plainly.'] as Map<String, Object>
        ]

        when:
        String finalPrompt = service.finalWriterUserPrompt(run(), plan(), context, [readEvidence()], [])

        then:
        finalPrompt.contains('You are an assistant inside Mauro Data Mapper.')
        finalPrompt.contains('Do not invent facts about Mauro')
        finalPrompt.contains('Answer in clear language.')
        finalPrompt.contains('Mention limitations plainly.')
        !finalPrompt.contains('Use mauro_get and mauro_describe')
        !finalPrompt.contains('Recommended skills')
        !finalPrompt.contains('Context requests')
        !finalPrompt.contains('Planning hints')
        !finalPrompt.contains('Call mauro_get with guidance')
    }

    void 'persona asset stays free of agentic lookup policy'() {
        given:
        String persona = getClass().classLoader.getResource('META-INF/mauro/chat/assets/personas/mauro-catalogue.yml').text

        expect:
        persona.contains('You are an assistant inside Mauro Data Mapper.')
        persona.contains('Answer in clear language.')
        !persona.contains('use available context and tools to ground your interpretation')
        !persona.contains('Use retrieved skills and tool results')
        !persona.contains('Read-only and non-destructive lookup tools')
        !persona.contains('keep going')
        !persona.contains('look it up')
    }

    void 'context resolver system prompt renders explicit mode'() {
        given:
        AgentPromptService service = new AgentPromptService(new ChatPromptComposer(null))

        when:
        String initial = service.contextResolverSystemPromptRender(['mauro_search'], null).text
        String replan = service.contextResolverSystemPromptRender(['mauro_search'], 'Need a concrete read step').text

        then:
        initial.contains('Context mode: initial planning.')
        initial.contains('No replan reason is active.')
        initial.contains('Use available context, retrieved skills, tool results, and advertised affordances to ground Mauro-specific interpretation before planning.')
        initial.contains('prefer safe read-only lookup')
        !initial.contains('Context mode: replan.')
        replan.contains('Context mode: replan.')
        replan.contains('Replan reason is supplied in the user prompt.')
    }

    void 'executor system prompt renders tool or no-tool mode'() {
        given:
        AgentPromptService service = new AgentPromptService(new ChatPromptComposer(null))

        when:
        String toolPrompt = service.executorSystemPrompt(toolStep())
        String analysisPrompt = service.executorSystemPrompt(analysisStep())

        then:
        toolPrompt.contains('Execution mode: tool step.')
        toolPrompt.contains('Emit one structured tool call using one allowed tool')
        !toolPrompt.contains('Execution mode: no-tool step.')
        analysisPrompt.contains('Execution mode: no-tool step.')
        analysisPrompt.contains('Do not call tools.')
        !analysisPrompt.contains('Emit one structured tool call using one allowed tool')
    }

    private static AgentRunRecord run() {
        new AgentRunRecord(
            id: 'run-1',
            sessionId: 'session-1',
            messageId: 'message-1',
            goal: 'Compare the adult and childrens diabetes eye assessment forms',
            model: 'gpt-5'
        )
    }

    private static AgentPlanRecord plan() {
        new AgentPlanRecord(
            id: 'plan-1',
            runId: 'run-1',
            goalRestatement: 'Compare two diabetes eye assessment forms',
            successCriteria: ['Evidence: forms identified', 'Comparison: differences stated']
        )
    }

    private static AgentContextRecord context() {
        new AgentContextRecord(
            id: 'context-1',
            runId: 'run-1',
            goalRestatement: 'Compare two diabetes eye assessment forms',
            resolvedResources: [[
                label: 'Adult Diabetes Eye Assessment Form',
                id: 'adult-eye',
                domainType: 'DataModel',
                uri: 'mauro-api://http-get/api/dataModels/adult-eye'
            ] as Map<String, Object>],
            planningHints: ['Read each resolved form before comparing.'],
            constraints: ['Do not broaden to all diabetes forms.']
        )
    }

    private static AgentStepRecord toolStep() {
        new AgentStepRecord(
            id: 'step-search',
            runId: 'run-1',
            planId: 'plan-1',
            title: 'Search forms',
            objective: 'Find the named diabetes eye forms',
            kind: 'search',
            allowedTools: ['mauro_search'],
            expectedOutput: 'Search result rows',
            successCriteria: ['Tool: mauro_search called']
        )
    }

    private static AgentStepRecord analysisStep() {
        new AgentStepRecord(
            id: 'step-compare',
            runId: 'run-1',
            planId: 'plan-1',
            title: 'Compare forms',
            objective: 'Compare the gathered form evidence',
            kind: 'analysis',
            allowedTools: [],
            expectedOutput: 'Terse comparison',
            successCriteria: ['Analysis: differences stated']
        )
    }

    private static AgentEvidenceRecord searchEvidence() {
        new AgentEvidenceRecord(
            id: 'evidence-search',
            runId: 'run-1',
            stepId: 'step-search',
            sourceType: 'tool_result',
            sourceName: 'mauro_search',
            sourceId: 'call-search',
            title: 'Tool result mauro_search',
            summary: 'Tool mauro_search completed.',
            content: 'FULL_EVIDENCE_BODY_WITH_DETAILS_SHOULD_ONLY_REACH_ANALYSIS_STEPS',
            structuredContent: [
                callId: 'call-search',
                ok: true,
                output: [
                    tool: 'mauro_search',
                    arguments: [searchTerm: 'diabetes eye forms', domainTypes: ['DataModel']],
                    output: [
                        count: 2,
                        items: [
                            [
                                label: 'Adult Diabetes Eye Assessment Form',
                                id: 'adult-eye',
                                domainType: 'DataModel',
                                readUri: 'mauro-api://http-get/api/dataModels/adult-eye'
                            ],
                            [
                                label: 'Childrens Diabetes Eye Assessment Form',
                                id: 'child-eye',
                                domainType: 'DataModel',
                                readUri: 'mauro-api://http-get/api/dataModels/child-eye'
                            ]
                        ]
                    ]
                ]
            ] as Map<String, Object>,
            createdAt: Instant.now(),
            metadata: [
                callId: 'call-search',
                ok: true,
                evidenceRole: 'tool_result',
                pertinentToFinal: true
            ] as Map<String, Object>
        )
    }

    private static AgentEvidenceRecord readEvidence() {
        new AgentEvidenceRecord(
            id: 'evidence-read',
            runId: 'run-1',
            stepId: 'step-read',
            sourceType: 'tool_result',
            sourceName: 'mauro_get',
            sourceId: 'call-read',
            title: 'Tool result mauro_get',
            summary: 'Tool mauro_get completed.',
            content: '{"id":"adult-eye","label":"Adult Diabetes Eye Assessment Form","dataClasses":[{"label":"Clinical details","dataElements":[{"label":"HbA1c"}]}]}',
            structuredContent: [
                callId: 'call-read',
                ok: true,
                output: [
                    tool: 'mauro_get',
                    output: [
                        id: 'adult-eye',
                        label: 'Adult Diabetes Eye Assessment Form',
                        dataClasses: [[
                            label: 'Clinical details',
                            dataElements: [[label: 'HbA1c']]
                        ]]
                    ]
                ]
            ] as Map<String, Object>,
            createdAt: Instant.now(),
            metadata: [
                callId: 'call-read',
                ok: true,
                evidenceRole: 'tool_result',
                pertinentToFinal: true
            ] as Map<String, Object>
        )
    }

    private static AgentEvidenceRecord readEvidenceWithLateStructure() {
        String padding = 'x' * 5000
        new AgentEvidenceRecord(
            id: 'evidence-read-large',
            runId: 'run-1',
            stepId: 'step-read',
            sourceType: 'tool_result',
            sourceName: 'mauro_get',
            sourceId: 'call-read-large',
            title: 'Tool result mauro_get',
            summary: 'Tool mauro_get completed.',
            content: "ID: adult-eye\nLabel: Adult Diabetes Eye Assessment Form\nReturned Data:\n${padding}\nLate section marker\n${padding}\nLate field marker",
            structuredContent: [
                callId: 'call-read-large',
                ok: true,
                output: [
                    tool: 'mauro_get',
                    output: [
                        id: 'adult-eye',
                        label: 'Adult Diabetes Eye Assessment Form'
                    ]
                ]
            ] as Map<String, Object>,
            createdAt: Instant.now(),
            metadata: [
                callId: 'call-read-large',
                ok: true,
                evidenceRole: 'tool_result',
                pertinentToFinal: true
            ] as Map<String, Object>
        )
    }

    private static List<Map<String, Object>> tools() {
        [[
            type: 'function',
            function: [
                name: 'mauro_search',
                description: 'Search Mauro catalogue',
                parameters: [type: 'object']
            ]
        ] as Map<String, Object>]
    }
}
