package org.maurodata.service.chat.llm

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
final class ToolCallAccumulator {

    @CompileStatic
    static final class ToolCallState {
        final Integer index
        String callId
        String toolType
        String functionName
        final StringBuilder argumentsBuilder = new StringBuilder(256)

        ToolCallState(final Integer index) {
            this.index = index
        }
    }

    @CompileStatic
    static final class CompletedToolCall {
        final Integer index
        final String callId
        final String toolType
        final String functionName
        final String argumentsRaw
        final Map<String, Object> argumentsJson

        CompletedToolCall(
            final Integer index,
            final String callId,
            final String toolType,
            final String functionName,
            final String argumentsRaw,
            final Map<String, Object> argumentsJson
        ) {
            this.index = index
            this.callId = callId
            this.toolType = toolType
            this.functionName = functionName
            this.argumentsRaw = argumentsRaw
            this.argumentsJson = argumentsJson
        }
    }

    private final Map<String, ToolCallState> statesByKey = new LinkedHashMap<String, ToolCallState>()
    private final JsonSlurper slurper = new JsonSlurper()

    void applyDelta(
        final Integer index,
        final String callId,
        final String toolType,
        final String functionName,
        final String argumentsDelta
    ) {
        if (index == null) {
            return
        }

        final String key = stateKey(index, callId)
        ToolCallState state = statesByKey.get(key)
        if (state == null) {
            state = new ToolCallState(index)
            statesByKey.put(key, state)
        }

        if (callId != null && !callId.isEmpty()) {
            state.callId = callId
        }
        if (toolType != null && !toolType.isEmpty()) {
            state.toolType = toolType
        }
        if (functionName != null && !functionName.isEmpty()) {
            state.functionName = functionName
        }
        if (argumentsDelta != null && !argumentsDelta.isEmpty()) {
            state.argumentsBuilder.append(argumentsDelta)
        }
    }

    List<CompletedToolCall> completeAll() {
        final List<CompletedToolCall> out = new ArrayList<CompletedToolCall>(statesByKey.size())
        for (Map.Entry<String, ToolCallState> entry : statesByKey.entrySet()) {
            final ToolCallState state = entry.getValue()
            final String argsRaw = state.argumentsBuilder.toString()

            Map<String, Object> parsedArgs = Collections.<String, Object>emptyMap()
            if (!argsRaw.isEmpty()) {
                final Object parsed = slurper.parseText(argsRaw)
                if (parsed instanceof Map) {
                    @SuppressWarnings('unchecked')
                    final Map<String, Object> typed = (Map<String, Object>) parsed
                    parsedArgs = typed
                } else {
                    throw new IllegalStateException(
                        "Tool call arguments must be a JSON object. index=${state.index}, value=${argsRaw}"
                    )
                }
            }

            out.add(
                new CompletedToolCall(
                    state.index,
                    state.callId,
                    state.toolType,
                    state.functionName,
                    argsRaw,
                    parsedArgs
                )
            )
        }

        statesByKey.clear()
        return out
    }

    boolean hasAny() {
        return !statesByKey.isEmpty()
    }

    void clear() {
        statesByKey.clear()
    }

    private static String stateKey(final Integer index, final String callId) {
        if (callId != null && !callId.trim().isEmpty()) {
            return 'id:' + callId
        }
        'index:' + index
    }
}
