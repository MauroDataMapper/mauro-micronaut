package org.maurodata.service.chat

import spock.lang.Specification

class ChatPromptAssetRegistryServiceSpec extends Specification {

    void 'loads existing skill yaml files as prompt assets'() {
        given:
        ChatPromptAssetRegistryService service = new ChatPromptAssetRegistryService(new ChatPromptAssetDefinitionLoader())

        when:
        List<ChatPromptAssetDefinition> assets = service.listAssets()

        then:
        assets.find {it.id == 'mauro-catalogue'}.type == 'PERSONA'
        assets.find {it.id == 'mauro-form-representation'}.type == 'SKILL'
        assets.find {it.id == 'mauro-form-representation'}.instruction.contains('Search parameter rules')
        assets.find {it.id == 'mauro-form-representation'}.toolApplicability.find {it.tool == 'mauro_search'}.relationship == 'REQUIRED_PREREQUISITE'
        assets.find {it.id == 'agent-planner-user'}.type == 'ROLE_PROMPT'
        assets.find {it.id == 'agent-final-writer-user'}.instruction.contains('Final-answer tool guidance to follow')
        assets.find {it.id == 'fragment-agent-goal-scope'}.type == 'FRAGMENT'
        assets.find {it.id == 'fragment-agent-goal-scope'}.keywords.isEmpty()
        assets.find {it.id == 'fragment-agent-goal-scope'}.priority == null
        assets.find {it.id == 'agent-planner-system'}.fragments == [
            'fragment-agent-strict-json',
            'fragment-agent-goal-scope',
            'fragment-agent-catalogue-evidence',
            'fragment-agent-tool-recovery'
        ]
        assets.find {it.id == 'agent-step-evaluator-system'}.fragments.contains('fragment-agent-step-evaluator-decision-policy')
        assets.find {it.id == 'agent-plan-evaluator-system'}.fragments.contains('fragment-agent-plan-evaluator-decision-policy')
        assets.find {it.id == 'agent-executor-system'}.fragments == ['fragment-agent-tool-recovery']
    }

    void 'loads tool policy as a prompt asset and legacy prompt resource'() {
        given:
        ChatPromptAssetRegistryService assetService = new ChatPromptAssetRegistryService(new ChatPromptAssetDefinitionLoader())
        ChatPromptResourceService resourceService = new ChatPromptResourceService(assetService)

        expect:
        assetService.findAsset('tool-policy').type == 'POLICY_FRAGMENT'
        resourceService.getPrompt(ChatPromptResourceService.TOOL_POLICY).contains('Do not describe, simulate, or print tool calls in prose.')
    }

    void 'prompt assets support direct skill and persona discovery'() {
        given:
        ChatPromptAssetRegistryService assetService = new ChatPromptAssetRegistryService(new ChatPromptAssetDefinitionLoader())

        expect:
        assetService.listAssetsByType('PERSONA')*.id.contains('mauro-catalogue')
        assetService.searchAssets('forms')*.id.contains('mauro-form-representation')
        assetService.findAsset('mauro-form-representation').routing.toolName == 'mauro_skill'
        assetService.findAsset('mauro-form-representation').sourcePath.endsWith('/mauro-form-representation.yml')
    }

    void 'prompt composer expands explicitly declared fragments'() {
        given:
        ChatPromptAssetRegistryService assetService = new ChatPromptAssetRegistryService(new ChatPromptAssetDefinitionLoader())
        ChatPromptComposer composer = new ChatPromptComposer(assetService)

        when:
        ChatPromptRenderResult result = composer.renderResult('agent-planner-system', [toolNames: 'mauro_search'] as Map<String, Object>)
        String rendered = result.text

        then:
        rendered.contains('Return strict JSON only. Do not wrap it in markdown.')
        rendered.contains('Preserve the user\'s scope and do not add "all", "every", "complete", "exhaustive", or equivalent requirements unless the user explicitly asked for them.')
        rendered.contains('Search result snippets, labels, and IDs are discovery evidence only.')
        rendered.contains('Do not hallucinate resource URIs from UUIDs.')
        rendered.contains('You are the planner for a Mauro catalogue agent.')
        result.assetId == 'agent-planner-system'
        result.assetVersion == '1.0.0'
        result.fragments*.id == [
            'fragment-agent-strict-json',
            'fragment-agent-goal-scope',
            'fragment-agent-catalogue-evidence',
            'fragment-agent-tool-recovery'
        ]
        result.variableNames == ['toolNames']
        result.redactedVariableNames == []
    }
}
