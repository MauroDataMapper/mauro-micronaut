package org.maurodata.service.chat.llm

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.maurodata.api.chat.ToolInvokeRequest
import org.maurodata.api.chat.ToolInvokeResponse
import org.maurodata.service.chat.ChatMcpService
import org.reactivestreams.Publisher

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import reactor.core.publisher.Flux

@CompileStatic
@Singleton
class OpenAiProvider implements LlmProvider {

    private final String baseUrl
    private final String apiKey
    private final ProviderStreamDecoders.OpenAiSseDecoder decoder
    private final ChatMcpService mcpService

    OpenAiProvider(
        @Value('${chat.providers.openai.base-url:https://api.openai.com}') final String baseUrl,
        @Value('${chat.providers.openai.api-key:}') final String apiKey,
        final ChatMcpService mcpService
    ) {
        this.baseUrl = baseUrl
        this.apiKey = apiKey
        this.decoder = new ProviderStreamDecoders.OpenAiSseDecoder()
        this.mcpService = mcpService
    }

    @Override
    String id() {
        'openai'
    }

    @Override
    Publisher<ProviderChunk> streamChat(final ProviderRequest request) {
        return Flux.create({ reactor.core.publisher.FluxSink<ProviderChunk> sink ->
            final Runnable task = new Runnable() {
                @Override
                void run() {
                    try {
                        if (request.model == null || request.model.trim().isEmpty()) {
                            sink.next(new ProviderChunk('error', request.messageId, 'Missing model for OpenAI request', Collections.<String, Object>emptyMap()))
                            return
                        }
                        if (apiKey == null || apiKey.trim().isEmpty()) {
                            sink.next(new ProviderChunk('error', request.messageId, 'Missing OPENAI_API_KEY', Collections.<String, Object>emptyMap()))
                            return
                        }

                        final List<ProviderMessage> workingMessages = new ArrayList<ProviderMessage>(request.messages)

                        boolean continueLoop = true
                        while (continueLoop) {
                            final OpenAiTurnResult turn = streamOneTurn(request, workingMessages, sink)
                            if (!turn.toolCalls.isEmpty()) {
                                ProviderMessage assistantToolCallMessage = buildAssistantToolCallMessage(turn.toolCalls)
                                if (assistantToolCallMessage != null) {
                                    workingMessages.add(assistantToolCallMessage)
                                    emitProviderMessage(sink, request.messageId, assistantToolCallMessage)
                                }
                                for (int i = 0; i < turn.toolCalls.size(); i++) {
                                    final ToolCallAccumulator.CompletedToolCall call = turn.toolCalls.get(i)
                                    handleToolCall(call, workingMessages, request.messageId, sink)
                                }
                            } else {
                                continueLoop = false
                            }
                        }
                    } catch (final Throwable t) {
                        sink.next(new ProviderChunk('error', request.messageId, t.getMessage(), Collections.<String, Object>emptyMap()))
                    } finally {
                        sink.complete()
                    }
                }
            }

            final Thread thread = new Thread(task, 'openai-stream-' + request.sessionId)
            thread.setDaemon(true)
            thread.start()
        })
    }

    private OpenAiTurnResult streamOneTurn(
        final ProviderRequest request,
        final List<ProviderMessage> workingMessages,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink
    ) throws Exception {
        final ToolCallAccumulator accumulator = new ToolCallAccumulator()
        final String requestBody = buildRequestBody(request, workingMessages)

        final HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + '/v1/chat/completions'))
            .header('Authorization', 'Bearer ' + apiKey)
            .header('Content-Type', 'application/json')
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        final HttpResponse<InputStream> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            final String errorBody = readBodySnippet(response.body())
            throw new IllegalStateException("OpenAI HTTP ${response.statusCode()}${errorBody ? ': ' + errorBody : ''}")
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), 'UTF-8'))

        try {
            String line = reader.readLine()
            while (line != null) {
                final List<ProviderChunk> decoded = decoder.decodeDataLine(line, request.messageId)
                for (int i = 0; i < decoded.size(); i++) {
                    final ProviderChunk chunk = decoded.get(i)
                    if ('token'.equals(chunk.type)) {
                        sink.next(chunk)
                    } else if ('tool_call'.equals(chunk.type)) {
                        applyToolDelta(accumulator, chunk.metadata)
                    } else if ('error'.equals(chunk.type)) {
                        sink.next(chunk)
                    }
                }
                line = reader.readLine()
            }
        } finally {
            reader.close()
        }

        final List<ToolCallAccumulator.CompletedToolCall> calls = accumulator.hasAny()
            ? accumulator.completeAll()
            : Collections.<ToolCallAccumulator.CompletedToolCall>emptyList()

        return new OpenAiTurnResult(calls)
    }

    private void applyToolDelta(
        final ToolCallAccumulator accumulator,
        final Map<String, Object> metadata
    ) {
        final Integer index = asInteger(metadata.get('index'))
        final String callId = asString(metadata.get('callId'))
        final String toolType = asString(metadata.get('toolType'))
        final String functionName = asString(metadata.get('name'))
        final String argumentsDelta = asString(metadata.get('argumentsDelta'))

        accumulator.applyDelta(index, callId, toolType, functionName, argumentsDelta)
    }

    private void handleToolCall(
        final ToolCallAccumulator.CompletedToolCall call,
        final List<ProviderMessage> workingMessages,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink
    ) {
        final String toolName = call.functionName
        if (toolName == null || toolName.isEmpty()) {
            sink.next(new ProviderChunk('error', messageId, 'Tool call missing function name', Collections.<String, Object>emptyMap()))
            return
        }

        final Map<String, Object> toolMeta = new LinkedHashMap<String, Object>(3)
        toolMeta.put('callId', call.callId)
        toolMeta.put('name', toolName)
        toolMeta.put('arguments', call.argumentsJson)
        sink.next(new ProviderChunk('tool_call', messageId, null, toolMeta))

        final ToolInvokeRequest invokeRequest = new ToolInvokeRequest(arguments: call.argumentsJson)
        final ToolInvokeResponse invokeResponse = mcpService.invokeTool(toolName, invokeRequest)

        final Map<String, Object> resultMeta = new LinkedHashMap<String, Object>(3)
        resultMeta.put('callId', call.callId)
        resultMeta.put('ok', invokeResponse.success)
        resultMeta.put('output', invokeResponse.result)
        final String invocationId = extractInvocationId(invokeResponse)
        if (invocationId != null) {
            resultMeta.put('invocationId', invocationId)
        }
        if (invokeResponse.error != null) {
            resultMeta.put('error', invokeResponse.error)
        }
        sink.next(new ProviderChunk('tool_result', messageId, null, resultMeta))

        final String toolResultJson = JsonOutput.toJson([
            callId: call.callId,
            invocationId: invocationId,
            ok    : invokeResponse.success,
            output: invokeResponse.result,
            error : invokeResponse.error
        ])
        ProviderMessage toolMessage = new ProviderMessage('tool', invokeResponse.modelText ?: toolResultJson, call.callId, toolName)
        workingMessages.add(toolMessage)
        emitProviderMessage(sink, messageId, toolMessage)
    }

    private String buildRequestBody(final ProviderRequest request, final List<ProviderMessage> messages) {
        final List<Map<String, Object>> wireMessages = new ArrayList<Map<String, Object>>(messages.size())
        for (int i = 0; i < messages.size(); i++) {
            final ProviderMessage m = messages.get(i)
            final Map<String, Object> wm = new LinkedHashMap<String, Object>(4)
            wm.put('role', m.role)
            wm.put('content', m.content)
            if (m.toolCallId != null) {
                wm.put('tool_call_id', m.toolCallId)
            }
            if (m.name != null) {
                wm.put('name', m.name)
            }
            if (m.toolCalls != null && !m.toolCalls.isEmpty()) {
                wm.put('tool_calls', m.toolCalls)
            }
            wireMessages.add(wm)
        }

        final Map<String, Object> body = new LinkedHashMap<String, Object>(5)
        body.put('model', request.model)
        body.put('stream', Boolean.TRUE)
        body.put('messages', wireMessages)
        body.put('tools', request.tools != null ? request.tools : Collections.<Map<String, Object>>emptyList())
        if (request.options != null && !request.options.isEmpty()) {
            body.putAll(request.options)
        }

        return JsonOutput.toJson(body)
    }

    private static ProviderMessage buildAssistantToolCallMessage(final List<ToolCallAccumulator.CompletedToolCall> callsToRun) {
        if (callsToRun == null || callsToRun.isEmpty()) {
            return null
        }
        final List<Map<String, Object>> toolCalls = new ArrayList<Map<String, Object>>(callsToRun.size())
        for (int i = 0; i < callsToRun.size(); i++) {
            final ToolCallAccumulator.CompletedToolCall call = callsToRun.get(i)
            if (call.functionName == null || call.functionName.trim().isEmpty()) {
                continue
            }
            toolCalls.add([
                id      : call.callId,
                type    : call.toolType ?: 'function',
                function: [
                    name     : call.functionName,
                    arguments: JsonOutput.toJson(call.argumentsJson ?: Collections.<String, Object>emptyMap())
                ] as Map<String, Object>
            ] as Map<String, Object>)
        }
        if (toolCalls.isEmpty()) {
            return null
        }
        new ProviderMessage(
            role: 'assistant',
            content: '',
            toolCalls: toolCalls
        )
    }

    private static void emitProviderMessage(
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final String messageId,
        final ProviderMessage message
    ) {
        if (sink == null || message == null) {
            return
        }
        sink.next(new ProviderChunk('provider_request_message', messageId, null, [
            providerMessage: providerMessageToMap(message)
        ] as Map<String, Object>))
    }

    private static Map<String, Object> providerMessageToMap(final ProviderMessage message) {
        final Map<String, Object> out = new LinkedHashMap<String, Object>()
        out.put('role', message.role)
        out.put('content', message.content ?: '')
        if (message.toolCallId != null) {
            out.put('toolCallId', message.toolCallId)
        }
        if (message.name != null) {
            out.put('name', message.name)
        }
        if (message.toolCalls != null && !message.toolCalls.isEmpty()) {
            out.put('toolCalls', message.toolCalls)
        }
        out
    }

    private static String asString(final Object value) {
        return value == null ? null : String.valueOf(value)
    }

    private static Integer asInteger(final Object value) {
        if (value instanceof Integer) return (Integer) value
        if (value instanceof Number) return Integer.valueOf(((Number) value).intValue())
        if (value instanceof String && !((String) value).isEmpty()) return Integer.valueOf((String) value)
        return null
    }

    private static String extractInvocationId(final ToolInvokeResponse response) {
        if (response == null || response.result == null) {
            return null
        }
        final Object value = response.result.get('invocationId')
        return value == null ? null : String.valueOf(value)
    }

    private static String readBodySnippet(final InputStream inputStream) {
        if (inputStream == null) {
            return null
        }
        final byte[] bytes = inputStream.readAllBytes()
        if (bytes == null || bytes.length == 0) {
            return null
        }
        final String text = new String(bytes, StandardCharsets.UTF_8)
        final String trimmed = text.trim()
        if (trimmed.length() <= 500) {
            return trimmed
        }
        return trimmed.substring(0, 500)
    }

    @CompileStatic
    private static final class OpenAiTurnResult {
        final List<ToolCallAccumulator.CompletedToolCall> toolCalls

        OpenAiTurnResult(final List<ToolCallAccumulator.CompletedToolCall> toolCalls) {
            this.toolCalls = toolCalls
        }
    }
}
