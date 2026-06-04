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

    private final Map<Integer, ToolCallState> statesByIndex = new LinkedHashMap<Integer, ToolCallState>()
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

        ToolCallState state = statesByIndex.get(index)
        if (state == null) {
            state = new ToolCallState(index)
            statesByIndex.put(index, state)
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
        final List<CompletedToolCall> out = new ArrayList<CompletedToolCall>(statesByIndex.size())
        for (Map.Entry<Integer, ToolCallState> entry : statesByIndex.entrySet()) {
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

        statesByIndex.clear()
        return out
    }

    boolean hasAny() {
        return !statesByIndex.isEmpty()
    }

    void clear() {
        statesByIndex.clear()
    }
}
