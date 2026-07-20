package org.maurodata.service.chat.llm

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
final class ProviderStreamDecoders {

    private ProviderStreamDecoders() {
    }

    @CompileStatic
    static final class OllamaStreamDecoder {
        private final JsonSlurper slurper = new JsonSlurper()

        List<ProviderChunk> decodeLine(final String line, final String assistantMessageId) {
            if (line == null || line.trim().isEmpty()) {
                return Collections.<ProviderChunk>emptyList()
            }

            final Object parsed = slurper.parseText(line)
            if (!(parsed instanceof Map)) {
                return Collections.<ProviderChunk>emptyList()
            }

            final Map<?, ?> root = (Map<?, ?>) parsed
            final Object messageObj = root.get('message')
            final String model = toNullableString(root.get('model'))
            final Boolean done = toNullableBoolean(root.get('done'))

            String content = null
            if (messageObj instanceof Map) {
                final Map<?, ?> message = (Map<?, ?>) messageObj
                content = toNullableString(message.get('content'))
            }

            final List<ProviderChunk> out = new ArrayList<ProviderChunk>(3)

            if (content != null && !content.isEmpty()) {
                final Map<String, Object> metadata = new LinkedHashMap<String, Object>(1)
                if (model != null) {
                    metadata.put('model', model)
                }
                out.add(new ProviderChunk('token', assistantMessageId, content, metadata))
            }

            if (Boolean.TRUE == done) {
                out.add(new ProviderChunk('message_complete', assistantMessageId, null, Collections.<String, Object>emptyMap()))
                out.add(new ProviderChunk('done', assistantMessageId, null, Collections.<String, Object>emptyMap()))
            }

            return out
        }
    }

    @CompileStatic
    static final class OpenAiSseDecoder {
        private final JsonSlurper slurper = new JsonSlurper()

        List<ProviderChunk> decodeDataLine(final String line, final String assistantMessageId) {
            if (line == null || !line.startsWith('data:')) {
                return Collections.<ProviderChunk>emptyList()
            }

            final String payload = line.substring(5).trim()
            if (payload.isEmpty()) {
                return Collections.<ProviderChunk>emptyList()
            }

            if ('[DONE]' == payload) {
                final List<ProviderChunk> doneChunks = new ArrayList<ProviderChunk>(2)
                doneChunks.add(new ProviderChunk('message_complete', assistantMessageId, null, Collections.<String, Object>emptyMap()))
                doneChunks.add(new ProviderChunk('done', assistantMessageId, null, Collections.<String, Object>emptyMap()))
                return doneChunks
            }

            final Object parsed = slurper.parseText(payload)
            if (!(parsed instanceof Map)) {
                return Collections.<ProviderChunk>emptyList()
            }

            final Map<?, ?> root = (Map<?, ?>) parsed
            final String model = toNullableString(root.get('model'))
            final Object choicesObj = root.get('choices')
            if (!(choicesObj instanceof List)) {
                return Collections.<ProviderChunk>emptyList()
            }

            final List<?> choices = (List<?>) choicesObj
            if (choices.isEmpty()) {
                return Collections.<ProviderChunk>emptyList()
            }

            final Object firstChoiceObj = choices.get(0)
            if (!(firstChoiceObj instanceof Map)) {
                return Collections.<ProviderChunk>emptyList()
            }

            final Map<?, ?> firstChoice = (Map<?, ?>) firstChoiceObj
            final Object deltaObj = firstChoice.get('delta')
            final String finishReason = toNullableString(firstChoice.get('finish_reason'))

            final List<ProviderChunk> out = new ArrayList<ProviderChunk>(3)

            if (deltaObj instanceof Map) {
                final Map<?, ?> delta = (Map<?, ?>) deltaObj
                final String content = toNullableString(delta.get('content'))
                if (content != null && !content.isEmpty()) {
                    final Map<String, Object> metadata = new LinkedHashMap<String, Object>(1)
                    if (model != null) {
                        metadata.put('model', model)
                    }
                    out.add(new ProviderChunk('token', assistantMessageId, content, metadata))
                }

                final Object toolCallsObj = delta.get('tool_calls')
                if (toolCallsObj instanceof List) {
                    final List<?> toolCalls = (List<?>) toolCallsObj
                    for (int i = 0; i < toolCalls.size(); i++) {
                        final Object toolCallObj = toolCalls.get(i)
                        if (toolCallObj instanceof Map) {
                            final Map<?, ?> tc = (Map<?, ?>) toolCallObj
                            final String callId = toNullableString(tc.get('id'))
                            final String type = toNullableString(tc.get('type'))
                            final Object functionObj = tc.get('function')

                            final Map<String, Object> metadata = new LinkedHashMap<String, Object>(4)
                            metadata.put('index', Integer.valueOf(i))
                            if (callId != null) metadata.put('callId', callId)
                            if (type != null) metadata.put('toolType', type)

                            if (functionObj instanceof Map) {
                                final Map<?, ?> functionMap = (Map<?, ?>) functionObj
                                final String functionName = toNullableString(functionMap.get('name'))
                                final String functionArguments = toNullableString(functionMap.get('arguments'))
                                if (functionName != null) metadata.put('name', functionName)
                                if (functionArguments != null) metadata.put('argumentsDelta', functionArguments)
                            }

                            out.add(new ProviderChunk('tool_call', assistantMessageId, null, metadata))
                        }
                    }
                }
            }

            if (finishReason != null && !finishReason.isEmpty()) {
                final Map<String, Object> metadata = new LinkedHashMap<String, Object>(1)
                metadata.put('finishReason', finishReason)
                out.add(new ProviderChunk('message_complete', assistantMessageId, null, metadata))
                out.add(new ProviderChunk('done', assistantMessageId, null, Collections.<String, Object>emptyMap()))
            }

            return out
        }
    }

    private static String toNullableString(final Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static Boolean toNullableBoolean(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value
        }
        if (value instanceof String) {
            return Boolean.valueOf((String) value)
        }
        null
    }
}
