package org.maurodata.plugin.chat.providers

import org.maurodata.service.chat.llm.*

import org.maurodata.plugin.chat.providers.*

import groovy.json.JsonSlurper
import org.maurodata.service.chat.ChatMcpService
import org.maurodata.service.chat.ChatPromptResourceService
import spock.lang.Specification

import java.lang.reflect.Method

class OllamaProviderRequestOptionsSpec extends Specification {

    void 'ollama request body adds dynamic context defaults when caller does not provide them'() {
        given:
        OllamaProvider provider = providerWithContextDefaults(8192, 32768, 1024)
        ProviderRequest request = new ProviderRequest(
            model: 'ministral',
            messages: [
                new ProviderMessage(role: 'system', content: 'a' * 18000),
                new ProviderMessage(role: 'user', content: 'Look up forms about maternity')
            ],
            options: [:]
        )

        when:
        Map body = buildRequestBody(provider, request)

        then:
        body.options.num_predict == 1024
        body.options.num_ctx >= 8192
        body.options.num_ctx <= 32768
        body.messages*.role == ['system', 'user']
    }

    void 'ollama request body preserves caller supplied context options'() {
        given:
        OllamaProvider provider = providerWithContextDefaults(8192, 32768, 1024)
        ProviderRequest request = new ProviderRequest(
            model: 'ministral',
            messages: [
                new ProviderMessage(role: 'system', content: 'context'),
                new ProviderMessage(role: 'user', content: 'Look up forms about maternity')
            ],
            options: [
                options: [num_ctx: 4096, num_predict: 128]
            ] as Map<String, Object>
        )

        when:
        Map body = buildRequestBody(provider, request)

        then:
        body.options.num_ctx == 4096
        body.options.num_predict == 128
    }

    void 'assistant tool call history preserves visible content emitted before structured tool call'() {
        given:
        ToolCallAccumulator.CompletedToolCall call = new ToolCallAccumulator.CompletedToolCall(
            0,
            'call_1',
            'function',
            'mauro_get',
            '{"uri":"mauro-api://http-get/api/dataModels/dm-1"}',
            [uri: 'mauro-api://http-get/api/dataModels/dm-1'] as Map<String, Object>
        )

        when:
        ProviderMessage message = buildAssistantToolCallMessage([call], 'Here are the search results as a table.')

        then:
        message.role == 'assistant'
        message.content == 'Here are the search results as a table.'
        message.toolCalls.size() == 1
        message.toolCalls[0].function.name == 'mauro_get'
    }

    void 'assistant tool call history strips text tool marker from visible content'() {
        when:
        String content = visibleAssistantContentBeforeToolCall(
            'Here are the search results.\n[TOOL_CALLS]mauro_get{"uri":"mauro-api://http-get/api/dataModels/dm-1"}'
        )

        then:
        content == 'Here are the search results.'
    }

    private OllamaProvider providerWithContextDefaults(int defaultNumCtx, int maxNumCtx, int defaultNumPredict) {
        new OllamaProvider(
            'http://localhost:11434',
            true,
            true,
            false,
            false,
            false,
            300L,
            defaultNumCtx,
            maxNumCtx,
            defaultNumPredict,
            4,
            300,
            Stub(ChatMcpService),
            new ChatPromptResourceService()
        )
    }

    @SuppressWarnings('unchecked')
    private static Map buildRequestBody(OllamaProvider provider, ProviderRequest request) {
        Method method = OllamaProvider.getDeclaredMethod('buildRequestBody', ProviderRequest, List, Boolean.TYPE, Boolean.TYPE, List)
        method.accessible = true
        String json = (String) method.invoke(provider, request, request.messages, false, false, [])
        (Map) new JsonSlurper().parseText(json)
    }

    private static ProviderMessage buildAssistantToolCallMessage(List<ToolCallAccumulator.CompletedToolCall> calls, String content) {
        Method method = OllamaProvider.getDeclaredMethod('buildAssistantToolCallMessage', List, String)
        method.accessible = true
        (ProviderMessage) method.invoke(null, calls, content)
    }

    private static String visibleAssistantContentBeforeToolCall(String assistantText) {
        Method method = OllamaProvider.getDeclaredMethod('visibleAssistantContentBeforeToolCall', String)
        method.accessible = true
        (String) method.invoke(null, assistantText)
    }
}
