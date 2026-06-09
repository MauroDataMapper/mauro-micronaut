package org.maurodata.service.chat.llm

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.maurodata.api.chat.ToolInvokeRequest
import org.maurodata.api.chat.ToolInvokeResponse
import org.maurodata.service.chat.ChatPromptResourceService
import org.maurodata.service.chat.ChatMcpService
import org.reactivestreams.Publisher

import java.nio.charset.StandardCharsets
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
@Singleton
class OllamaProvider implements LlmProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OllamaProvider.class)
    private static final Set<String> REUSABLE_TOOLS = new LinkedHashSet<String>(Arrays.asList('skill_lookup')).asImmutable()

    private final String baseUrl
    private final boolean toolsEnabled
    private final boolean defaultThink
    private final boolean traceWire
    private final boolean traceTurnDebug
    private final boolean traceThinking
    private final long thinkingCapabilityCacheSeconds
    private final ChatMcpService mcpService
    private final ChatPromptResourceService promptResourceService
    private final JsonSlurper slurper = new JsonSlurper()
    private final OllamaTextToolCallExtractor textToolCallExtractor = new OllamaTextToolCallExtractor()
    private final Map<String, ThinkingCapabilityCacheEntry> thinkingCapabilityCache = new ConcurrentHashMap<String, ThinkingCapabilityCacheEntry>()

    OllamaProvider(
        @Value('${chat.providers.ollama.base-url:http://localhost:11434}') final String baseUrl,
        @Value('${chat.providers.ollama.tools-enabled:true}') final boolean toolsEnabled,
        @Value('${chat.providers.ollama.default-think:true}') final boolean defaultThink,
        @Value('${chat.providers.ollama.trace-wire:false}') final boolean traceWire,
        @Value('${chat.providers.ollama.trace-turn-debug:false}') final boolean traceTurnDebug,
        @Value('${chat.providers.ollama.trace-thinking:false}') final boolean traceThinking,
        @Value('${chat.providers.ollama.thinking-capability-cache-seconds:300}') final long thinkingCapabilityCacheSeconds,
        final ChatMcpService mcpService,
        final ChatPromptResourceService promptResourceService
    ) {
        this.baseUrl = baseUrl
        this.toolsEnabled = toolsEnabled
        this.defaultThink = defaultThink
        this.traceWire = traceWire
        this.traceTurnDebug = traceTurnDebug
        this.traceThinking = traceThinking
        this.thinkingCapabilityCacheSeconds = thinkingCapabilityCacheSeconds
        this.mcpService = mcpService
        this.promptResourceService = promptResourceService
        LOG.info(
            'OLLAMA_CONFIG baseUrl={} toolsEnabled={} defaultThink={} legacyThinkDefault={} traceWire={} traceTurnDebug={} traceThinking={}',
            this.baseUrl,
            Boolean.valueOf(this.toolsEnabled),
            Boolean.valueOf(this.defaultThink),
            Boolean.valueOf(this.traceWire),
            Boolean.valueOf(this.traceTurnDebug),
            Boolean.valueOf(this.traceThinking)
        )
    }

    @Override
    String id() {
        'ollama'
    }

    @Override
    Publisher<ProviderChunk> streamChat(final ProviderRequest request) {
        return reactor.core.publisher.Flux.create({ reactor.core.publisher.FluxSink<ProviderChunk> sink ->
            final Thread worker = new Thread(new Runnable() {
                @Override
                void run() {
                    try {
                        if (request.model == null || request.model.trim().isEmpty()) {
                            sink.next(new ProviderChunk('error', request.messageId, 'Missing model for Ollama request', Collections.<String, Object>emptyMap()))
                            return
                        }
                        final String userPrompt = extractLatestUserPrompt(request.messages)
                        final boolean toolIntent = OllamaToolingPolicy.isToolIntent(userPrompt)
                        final List<ProviderMessage> workingMessages = new ArrayList<ProviderMessage>(request.messages)

                        int toolRound = 0
                        int toolErrors = 0
                        final Set<String> executedToolNames = new LinkedHashSet<String>()
                        final Set<String> executedToolCallKeys = new LinkedHashSet<String>()
                        boolean continueLoop = true
                        while (continueLoop) {
                            final TurnDebugAccumulator debug = new TurnDebugAccumulator()
                            final List<Map<String, Object>> availableTools = filterTools(request.tools, executedToolNames)
                            final Set<String> allowedToolNames = toolNamesFromTools(availableTools)
                            final OllamaTurnResult turn = streamOneTurn(request, workingMessages, sink, toolIntent, debug, availableTools)
                            debug.structuredToolCalls = turn.toolCalls.size()

                            final List<ToolCallAccumulator.CompletedToolCall> executableCalls = new ArrayList<ToolCallAccumulator.CompletedToolCall>()
                            for (int i = 0; i < turn.toolCalls.size(); i++) {
                                final ToolCallAccumulator.CompletedToolCall call = turn.toolCalls.get(i)
                                final String fn = call.functionName
                                if (fn != null && !fn.trim().isEmpty() && allowedToolNames.contains(fn)) {
                                    executableCalls.add(call)
                                }
                            }
                            debug.executableStructuredToolCalls = executableCalls.size()

                            List<ToolCallAccumulator.CompletedToolCall> callsToRun = executableCalls
                            boolean fallbackSynthesized = false
                            if (toolIntent && OllamaToolingPolicy.shouldRunFallbackExtractor(executableCalls.size())) {
                                debug.fallbackAttempted = true
                                final OllamaTextToolCallExtractor.ExtractedToolCall extracted =
                                    textToolCallExtractor.extract(turn.assistantText, allowedToolNames)
                                if (extracted != null) {
                                    debug.fallbackExtracted = true
                                    fallbackSynthesized = true
                                    final String syntheticCallId = 'call_' + UUID.randomUUID().toString().replace('-', '').substring(0, 12)
                                    final ToolCallAccumulator.CompletedToolCall synthetic =
                                        new ToolCallAccumulator.CompletedToolCall(
                                            Integer.valueOf(0),
                                            syntheticCallId,
                                            'function',
                                            extracted.name,
                                            extracted.rawJson,
                                            extracted.parameters
                                        )
                                    callsToRun = Collections.<ToolCallAccumulator.CompletedToolCall>singletonList(synthetic)
                                }
                                if (callsToRun.isEmpty() && toolIntent && turn.malformedStructuredCalls != null &&
                                    !turn.malformedStructuredCalls.isEmpty() && allowedToolNames.size() == 1) {
                                    final String inferredTool = allowedToolNames.iterator().next()
                                    final Map<String, Object> inferredParams = turn.malformedStructuredCalls.get(0)
                                    final String syntheticCallId = 'call_' + UUID.randomUUID().toString().replace('-', '').substring(0, 12)
                                    final ToolCallAccumulator.CompletedToolCall synthetic =
                                        new ToolCallAccumulator.CompletedToolCall(
                                            Integer.valueOf(0),
                                            syntheticCallId,
                                            'function',
                                            inferredTool,
                                            JsonOutput.toJson(inferredParams),
                                            inferredParams
                                        )
                                    callsToRun = Collections.<ToolCallAccumulator.CompletedToolCall>singletonList(synthetic)
                                    fallbackSynthesized = true
                                    debug.fallbackExtracted = true
                                }
                            }
                            int successfulToolExecutions = 0

                            if (!callsToRun.isEmpty()) {
                                toolRound++
                                ProviderMessage assistantToolCallMessage = buildAssistantToolCallMessage(callsToRun)
                                if (assistantToolCallMessage != null) {
                                    workingMessages.add(assistantToolCallMessage)
                                    emitProviderMessage(sink, request.messageId, assistantToolCallMessage)
                                }
                                for (int i = 0; i < callsToRun.size(); i++) {
                                    final ToolCallAccumulator.CompletedToolCall call = callsToRun.get(i)
                                    final String callKey = toolCallKey(call)
                                    if (executedToolCallKeys.contains(callKey)) {
                                        LOG.info('Ignored repeated tool call messageId={} toolName={} arguments={}', request.messageId, call.functionName, call.argumentsRaw)
                                        continue
                                    }
                                    final ToolExecResult exec = handleToolCallStrict(
                                        call, workingMessages, request.messageId, sink, fallbackSynthesized
                                    )
                                    if (exec.executed) {
                                        executedToolCallKeys.add(callKey)
                                        if (call.functionName != null && !call.functionName.trim().isEmpty() && !REUSABLE_TOOLS.contains(call.functionName)) {
                                            executedToolNames.add(call.functionName)
                                        }
                                        successfulToolExecutions++
                                        debug.successfulToolExecutions++
                                    } else if (exec.error) {
                                        toolErrors++
                                        debug.toolErrors++
                                    }
                                }
                            }
                            debug.assistantTextLength = turn.assistantText == null ? 0 : turn.assistantText.length()
                            if (traceTurnDebug) {
                                LOG.info('OLLAMA_TURN_DEBUG {}', debug.snapshot(request.sessionId, request.messageId))
                            }

                            continueLoop = ToolLoopGuards.shouldContinueToolLoop(
                                toolRound,
                                toolErrors,
                                successfulToolExecutions,
                                sink,
                                request.messageId
                            )
                        }
                    } catch (final Throwable t) {
                        sink.next(new ProviderChunk('error', request.messageId, t.getMessage(), Collections.<String, Object>emptyMap()))
                    } finally {
                        sink.complete()
                    }
                }
            }, 'ollama-stream-' + request.sessionId)

            worker.setDaemon(true)
            worker.start()
        })
    }

    private OllamaTurnResult streamOneTurn(
        final ProviderRequest request,
        final List<ProviderMessage> workingMessages,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final boolean toolIntent,
        final TurnDebugAccumulator debug,
        final List<Map<String, Object>> availableTools
    ) throws Exception {
        final ToolCallAccumulator accumulator = new ToolCallAccumulator()
        final List<Map<String, Object>> malformedStructuredCalls = new ArrayList<Map<String, Object>>()
        final StringBuilder assistantTextBuffer = new StringBuilder(512)
        final HeaderFilterState headerFilterState = new HeaderFilterState()
        final ThinkingState thinkingState = new ThinkingState()
        final Object requestThink = extractRequestThinkValue(request)
        final boolean requestedThink = resolveRequestedThink(request)
        final boolean thinkingCapability = isThinkingSupportedByApi(request.model)
        final boolean thinkEnabled = requestedThink && thinkingCapability
        if (traceThinking) {
            LOG.info(
                'OLLAMA_THINKING_MODE sessionId={} messageId={} model={} requestThink={} requestedThink={} resolvedThink={} thinkingCapability={}',
                request.sessionId,
                request.messageId,
                request.model,
                requestThink,
                Boolean.valueOf(requestedThink),
                Boolean.valueOf(thinkEnabled),
                Boolean.valueOf(thinkingCapability)
            )
        }
        final String body = buildRequestBody(request, workingMessages, toolIntent, thinkEnabled, availableTools)
        if (traceWire) {
            LOG.info('OLLAMA_WIRE_REQUEST sessionId={} messageId={} body={}', request.sessionId, request.messageId, body)
        }
        final HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + '/api/chat'))
            .header('Content-Type', 'application/json')
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        final HttpResponse<InputStream> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
        if (traceWire) {
            LOG.info('OLLAMA_WIRE_RESPONSE_STATUS sessionId={} messageId={} statusCode={}', request.sessionId, request.messageId, Integer.valueOf(response.statusCode()))
        }
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            final String errorBody = readBodySnippet(response.body())
            throw new IllegalStateException("Ollama HTTP ${response.statusCode()}${errorBody ? ': ' + errorBody : ''}")
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), 'UTF-8'))

        try {
            String line = reader.readLine()
            while (line != null) {
                if (traceWire) {
                    LOG.info('OLLAMA_WIRE_RESPONSE_LINE sessionId={} messageId={} line={}', request.sessionId, request.messageId, line)
                }
                decodeOllamaLine(
                    line, request.messageId, sink, accumulator, malformedStructuredCalls, assistantTextBuffer, toolIntent, debug, headerFilterState, thinkingState
                )
                line = reader.readLine()
            }
            if (thinkingState.inThinking) {
                sink.next(new ProviderChunk('thinking_end', request.messageId, '', Collections.<String, Object>emptyMap()))
                thinkingState.inThinking = false
                thinkingState.explicitThinkingMode = false
                thinkingState.forceThinkingUntilTagEnd = false
            }
        } finally {
            reader.close()
        }

        final List<ToolCallAccumulator.CompletedToolCall> calls =
            accumulator.hasAny() ? accumulator.completeAll() : Collections.<ToolCallAccumulator.CompletedToolCall>emptyList()

        return new OllamaTurnResult(calls, malformedStructuredCalls, assistantTextBuffer.toString())
    }

    private void decodeOllamaLine(
        final String line,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final ToolCallAccumulator accumulator,
        final List<Map<String, Object>> malformedStructuredCalls,
        final StringBuilder assistantTextBuffer,
        final boolean toolIntent,
        final TurnDebugAccumulator debug,
        final HeaderFilterState headerFilterState,
        final ThinkingState thinkingState
    ) {
        if (line == null || line.trim().isEmpty()) {
            return
        }

        final Object parsed = slurper.parseText(line)
        if (!(parsed instanceof Map)) {
            return
        }
        final Map<?, ?> root = (Map<?, ?>) parsed
        final String rootError = asString(root.get('error'))
        if (rootError != null && !rootError.trim().isEmpty()) {
            sink.next(new ProviderChunk('error', messageId, 'Ollama error: ' + rootError, Collections.<String, Object>emptyMap()))
            return
        }
        final Object messageObj = root.get('message')
        if (!(messageObj instanceof Map)) {
            return
        }
        final Map<?, ?> message = (Map<?, ?>) messageObj

        final String content = asString(message.get('content'))
        final String thinking = asString(message.get('thinking'))
        if (traceThinking) {
            LOG.info(
                'OLLAMA_THINKING_CHUNK messageId={} hasThinking={} thinkingLen={} hasContent={} contentLen={}',
                messageId,
                Boolean.valueOf(thinking != null),
                Integer.valueOf(thinking == null ? 0 : thinking.length()),
                Boolean.valueOf(content != null),
                Integer.valueOf(content == null ? 0 : content.length())
            )
        }

        if (thinking != null && !thinking.isEmpty()) {
            debug.rawTokenChunks++
            emitThinkingChunk(thinking, messageId, sink, debug, headerFilterState, thinkingState)
        }

        if (content != null && !content.isEmpty()) {
            debug.rawTokenChunks++
            routeThinkingAwareContent(content, messageId, sink, assistantTextBuffer, debug, headerFilterState, thinkingState)
        }

        final Object toolCallsObj = message.get('tool_calls')
        if (toolCallsObj instanceof List) {
            final List<?> toolCalls = (List<?>) toolCallsObj
            for (int i = 0; i < toolCalls.size(); i++) {
                final Object toolCallObj = toolCalls.get(i)
                if (toolCallObj instanceof Map) {
                    final Map<?, ?> tc = (Map<?, ?>) toolCallObj
                    final String callId = asString(tc.get('id'))
                    final Object functionObj = tc.get('function')

                    String functionName = null
                    String argumentsRaw = null
                    Map<String, Object> arguments = Collections.<String, Object>emptyMap()
                    if (functionObj instanceof Map) {
                        final Map<?, ?> fn = (Map<?, ?>) functionObj
                        functionName = asString(fn.get('name'))
                        final Object argsObj = fn.get('arguments')
                        if (argsObj instanceof Map) {
                            @SuppressWarnings('unchecked')
                            final Map<String, Object> castArgs = (Map<String, Object>) argsObj
                            malformedStructuredCalls.add(new LinkedHashMap<String, Object>(castArgs))
                            arguments = new LinkedHashMap<String, Object>(castArgs)
                            argumentsRaw = JsonOutput.toJson((Map<?, ?>) argsObj)
                        } else {
                            argumentsRaw = asString(argsObj)
                            arguments = parseArguments(argumentsRaw)
                        }
                    }
                    if (functionName == null || functionName.trim().isEmpty()) {
                        LOG.debug('Ignored malformed structured tool call with missing function name messageId={}', messageId)
                        continue
                    }

                    accumulator.applyDelta(
                        Integer.valueOf(i),
                        callId,
                        'function',
                        functionName,
                        argumentsRaw
                    )

                    final Map<String, Object> meta = new LinkedHashMap<String, Object>(3)
                    meta.put('index', Integer.valueOf(i))
                    if (callId != null) meta.put('callId', callId)
                    if (functionName != null) meta.put('name', functionName)
                    meta.put('arguments', arguments)
                    if (argumentsRaw != null) meta.put('argumentsRaw', argumentsRaw)
                    sink.next(new ProviderChunk('tool_call', messageId, null, meta))
                }
            }
        }
    }

    private ToolExecResult handleToolCallStrict(
        final ToolCallAccumulator.CompletedToolCall call,
        final List<ProviderMessage> workingMessages,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final boolean fallbackSynthesized
    ) {
        final String toolName = call.functionName
        if (toolName == null || toolName.isEmpty()) {
            LOG.debug('Ignored malformed executable tool call with missing function name messageId={}', messageId)
            return ToolExecResult.error()
        }

        try {
            if (fallbackSynthesized) {
                final Map<String, Object> toolMeta = new LinkedHashMap<String, Object>(4)
                toolMeta.put('index', call.index)
                toolMeta.put('callId', call.callId)
                toolMeta.put('name', toolName)
                toolMeta.put('arguments', call.argumentsJson ?: Collections.<String, Object>emptyMap())
                if (call.argumentsRaw != null) {
                    toolMeta.put('argumentsRaw', call.argumentsRaw)
                }
                sink.next(new ProviderChunk('tool_call', messageId, null, toolMeta))
            }

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

            final String toolResultText = buildToolResultTextForModel(toolName, invokeResponse)
            ProviderMessage toolMessage = new ProviderMessage('tool', toolResultText, null, toolName)
            workingMessages.add(toolMessage)
            emitProviderMessage(sink, messageId, toolMessage)
            ProviderMessage postToolMessage = new ProviderMessage(
                'system',
                promptResourceService.getPrompt(ChatPromptResourceService.POST_TOOL_RESULT),
                null,
                null
            )
            workingMessages.add(postToolMessage)
            emitProviderMessage(sink, messageId, postToolMessage)
            return ToolExecResult.executed()
        } catch (final Throwable t) {
            sink.next(new ProviderChunk('error', messageId, 'tool invocation failed: ' + t.getMessage(), Collections.<String, Object>emptyMap()))
            return ToolExecResult.error()
        }
    }

    private String buildRequestBody(
        final ProviderRequest request,
        final List<ProviderMessage> messages,
        final boolean toolIntent,
        final boolean thinkEnabled,
        final List<Map<String, Object>> availableTools
    ) {
        final List<Map<String, Object>> wireMessages = new ArrayList<Map<String, Object>>(messages.size())
        for (int i = 0; i < messages.size(); i++) {
            final ProviderMessage m = messages.get(i)
            final Map<String, Object> wm = new LinkedHashMap<String, Object>(4)
            wm.put('role', m.role)
            wm.put('content', m.content)
            if (m.name != null) {
                if ('tool'.equals(m.role)) {
                    wm.put('tool_name', m.name)
                } else {
                    wm.put('name', m.name)
                }
            }
            if (m.toolCallId != null) {
                wm.put('tool_call_id', m.toolCallId)
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
        if (toolsEnabled && availableTools != null && !availableTools.isEmpty()) {
            body.put('tools', availableTools)
        }
        if (request.options != null && !request.options.isEmpty()) {
            body.putAll(request.options)
        }
        body.put('think', Boolean.valueOf(thinkEnabled))

        return JsonOutput.toJson(body)
    }

    private static String asString(final Object value) {
        return value == null ? null : String.valueOf(value)
    }

    private static Map<String, Object> parseArguments(final String argumentsRaw) {
        if (argumentsRaw == null || argumentsRaw.trim().isEmpty()) {
            return Collections.<String, Object>emptyMap()
        }
        try {
            final Object parsed = new JsonSlurper().parseText(argumentsRaw)
            if (parsed instanceof Map) {
                @SuppressWarnings('unchecked')
                final Map<String, Object> typed = (Map<String, Object>) parsed
                return new LinkedHashMap<String, Object>(typed)
            }
        } catch (final Throwable ignored) {
            return Collections.<String, Object>emptyMap()
        }
        Collections.<String, Object>emptyMap()
    }

    private boolean resolveRequestedThink(final ProviderRequest request) {
        final Object value = extractRequestThinkValue(request)
        if (value != null) {
            if (value instanceof Boolean) {
                return ((Boolean) value).booleanValue()
            }
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value)
            }
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0
            }
        }
        return defaultThink
    }

    private Object extractRequestThinkValue(final ProviderRequest request) {
        if (request.options != null && request.options.containsKey('think')) {
            return request.options.get('think')
        }
        return null
    }

    private boolean isThinkingSupportedByApi(final String model) {
        if (model == null || model.trim().isEmpty()) {
            return false
        }
        final Instant now = Instant.now()
        final ThinkingCapabilityCacheEntry cached = thinkingCapabilityCache.get(model)
        if (cached != null && cached.expiresAt.isAfter(now)) {
            return cached.supported
        }
        final boolean supported = fetchThinkingCapability(model)
        final Instant expiresAt = now.plusSeconds(Math.max(30L, thinkingCapabilityCacheSeconds))
        thinkingCapabilityCache.put(model, new ThinkingCapabilityCacheEntry(supported, expiresAt))
        return supported
    }

    private boolean fetchThinkingCapability(final String model) {
        try {
            final String body = JsonOutput.toJson([model: model])
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + '/api/show'))
                .header('Content-Type', 'application/json')
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() < 200 || response.statusCode() > 299) {
                return false
            }
            final Object parsed = slurper.parseText(response.body())
            if (!(parsed instanceof Map)) {
                return false
            }
            final Map<?, ?> root = (Map<?, ?>) parsed
            final Object capabilitiesObj = root.get('capabilities')
            if (!(capabilitiesObj instanceof List)) {
                return false
            }
            final List<?> capabilities = (List<?>) capabilitiesObj
            for (int i = 0; i < capabilities.size(); i++) {
                final Object item = capabilities.get(i)
                if (item != null && 'thinking'.equalsIgnoreCase(String.valueOf(item))) {
                    return true
                }
            }
        } catch (final Throwable t) {
            if (traceThinking) {
                LOG.info('OLLAMA_THINKING_CAPABILITY_CHECK_FAILED model={} message={}', model, t.getMessage())
            }
        }
        return false
    }

    @CompileStatic
    private static final class ThinkingCapabilityCacheEntry {
        final boolean supported
        final Instant expiresAt

        ThinkingCapabilityCacheEntry(final boolean supported, final Instant expiresAt) {
            this.supported = supported
            this.expiresAt = expiresAt
        }
    }

    private static boolean isFilteredHeaderToken(final String content, final HeaderFilterState state) {
        final String trimmed = content == null ? null : content.trim()
        if ('<|start_header_id|>'.equals(trimmed)) {
            state.inHeader = true
            return true
        }
        if ('<|end_header_id|>'.equals(trimmed)) {
            state.inHeader = false
            return true
        }
        if ('<|eot_id|>'.equals(trimmed)) {
            return true
        }
        if (state.inHeader) {
            return true
        }
        return false
    }

    private void routeThinkingAwareContent(
        final String content,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final StringBuilder assistantTextBuffer,
        final TurnDebugAccumulator debug,
        final HeaderFilterState headerFilterState,
        final ThinkingState thinkingState
    ) {
        if (thinkingState.inThinking && thinkingState.explicitThinkingMode) {
            sink.next(new ProviderChunk('thinking_end', messageId, '', Collections.<String, Object>emptyMap()))
            thinkingState.inThinking = false
            thinkingState.explicitThinkingMode = false
            thinkingState.forceThinkingUntilTagEnd = false
        }

        final String input = thinkingState.carry.toString() + content
        thinkingState.carry.setLength(0)

        if (thinkingState.inThinking && thinkingState.forceThinkingUntilTagEnd) {
            final int closeForced = input.indexOf('</think>')
            if (closeForced >= 0) {
                final String thinkingSegment = input.substring(0, closeForced)
                emitThinkingSegment(thinkingSegment, messageId, sink, headerFilterState, debug)
                sink.next(new ProviderChunk('thinking_end', messageId, '', Collections.<String, Object>emptyMap()))
                thinkingState.inThinking = false
                thinkingState.explicitThinkingMode = false
                thinkingState.forceThinkingUntilTagEnd = false
                final String remainder = input.substring(closeForced + '</think>'.length())
                if (!remainder.isEmpty()) {
                    emitVisibleSegment(remainder, messageId, sink, assistantTextBuffer, headerFilterState, debug)
                }
                return
            }
            emitThinkingSegment(input, messageId, sink, headerFilterState, debug)
            return
        }

        int index = 0
        while (index < input.length()) {
            if (thinkingState.inThinking) {
                final int close = input.indexOf('</think>', index)
                if (close >= 0) {
                    final String thinkingSegment = input.substring(index, close)
                    emitThinkingSegment(thinkingSegment, messageId, sink, headerFilterState, debug)
                    sink.next(new ProviderChunk('thinking_end', messageId, '', Collections.<String, Object>emptyMap()))
                    thinkingState.inThinking = false
                    thinkingState.forceThinkingUntilTagEnd = false
                    index = close + '</think>'.length()
                    continue
                }

                final String tailHeld = holdTailForTag(input.substring(index), '</think>', thinkingState.carry)
                emitThinkingSegment(tailHeld, messageId, sink, headerFilterState, debug)
                break
            }

            final int open = input.indexOf('<think>', index)
            if (open >= 0) {
                final String visible = input.substring(index, open)
                emitVisibleSegment(visible, messageId, sink, assistantTextBuffer, headerFilterState, debug)
                sink.next(new ProviderChunk('thinking_start', messageId, '', Collections.<String, Object>emptyMap()))
                thinkingState.inThinking = true
                thinkingState.explicitThinkingMode = false
                thinkingState.forceThinkingUntilTagEnd = false
                index = open + '<think>'.length()
                continue
            }

            final String tailHeld = holdTailForTag(input.substring(index), '<think>', thinkingState.carry)
            emitVisibleSegment(tailHeld, messageId, sink, assistantTextBuffer, headerFilterState, debug)
            break
        }
    }

    private static String holdTailForTag(final String text, final String tag, final StringBuilder carry) {
        int maxPrefix = Math.min(tag.length() - 1, text.length())
        int holdLen = 0
        for (int len = maxPrefix; len >= 1; len--) {
            final String suffix = text.substring(text.length() - len)
            if (tag.startsWith(suffix)) {
                holdLen = len
                break
            }
        }
        if (holdLen > 0) {
            carry.append(text.substring(text.length() - holdLen))
            return text.substring(0, text.length() - holdLen)
        }
        return text
    }

    private void emitVisibleSegment(
        final String segment,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final StringBuilder assistantTextBuffer,
        final HeaderFilterState headerFilterState,
        final TurnDebugAccumulator debug
    ) {
        if (segment == null || segment.isEmpty()) {
            return
        }
        if (isFilteredHeaderToken(segment, headerFilterState) || OllamaToolingPolicy.isControlToken(segment)) {
            debug.filteredControlTokenChunks++
            return
        }
        sink.next(new ProviderChunk('token', messageId, segment, Collections.<String, Object>emptyMap()))
        assistantTextBuffer.append(segment)
        debug.publishedTokenChunks++
    }

    private void emitThinkingSegment(
        final String segment,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final HeaderFilterState headerFilterState,
        final TurnDebugAccumulator debug
    ) {
        if (segment == null || segment.isEmpty()) {
            return
        }
        if (isFilteredHeaderToken(segment, headerFilterState) || OllamaToolingPolicy.isControlToken(segment)) {
            debug.filteredControlTokenChunks++
            return
        }
        sink.next(new ProviderChunk('thinking_token', messageId, segment, Collections.<String, Object>emptyMap()))
    }

    private void emitThinkingChunk(
        final String content,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink,
        final TurnDebugAccumulator debug,
        final HeaderFilterState headerFilterState,
        final ThinkingState thinkingState
    ) {
        if (isFilteredHeaderToken(content, headerFilterState) || OllamaToolingPolicy.isControlToken(content)) {
            debug.filteredControlTokenChunks++
            return
        }
        if (!thinkingState.inThinking) {
            sink.next(new ProviderChunk('thinking_start', messageId, '', Collections.<String, Object>emptyMap()))
        }
        thinkingState.inThinking = true
        thinkingState.explicitThinkingMode = true
        sink.next(new ProviderChunk('thinking_token', messageId, content, Collections.<String, Object>emptyMap()))
    }

    private static String extractInvocationId(final ToolInvokeResponse response) {
        if (response == null || response.result == null) {
            return null
        }
        final Object value = response.result.get('invocationId')
        return value == null ? null : String.valueOf(value)
    }

    private static Map<String, Object> compactResultForModel(final Map<String, Object> raw) {
        if (raw == null) {
            return Collections.<String, Object>emptyMap()
        }
        Map<String, Object> compact = new LinkedHashMap<String, Object>(raw)
        Object nestedOutput = compact.get('output')
        if (nestedOutput instanceof Map) {
            @SuppressWarnings('unchecked')
            Map<String, Object> nested = new LinkedHashMap<String, Object>((Map<String, Object>) nestedOutput)
            Object items = nested.get('items')
            if (items instanceof List) {
                List<?> list = (List<?>) items
                int maxItems = Math.min(list.size(), 10)
                List<Object> trimmed = new ArrayList<Object>(maxItems)
                for (int i = 0; i < maxItems; i++) {
                    trimmed.add(list.get(i))
                }
                nested.put('items', trimmed)
                nested.put('truncated', Boolean.valueOf(list.size() > maxItems))
                nested.put('returnedItems', Integer.valueOf(maxItems))
            }
            compact.put('output', nested)
        }
        compact
    }

    private static String buildToolResultTextForModel(
        final String toolName,
        final ToolInvokeResponse response
    ) {
        if (response == null) {
            return 'Tool result: no response was returned.'
        }
        if (!Boolean.TRUE.equals(response.success)) {
            return "Tool ${toolName} failed: ${response.error ?: 'unknown error'}"
        }
        if (response.modelText != null && !response.modelText.trim().isEmpty()) {
            return response.modelText
        }

        final Map<String, Object> result = response.result ?: Collections.<String, Object>emptyMap()
        return "Tool ${toolName} succeeded. Result:\n" + JsonOutput.prettyPrint(JsonOutput.toJson(compactResultForModel(result)))
    }

    private static ProviderMessage buildAssistantToolCallMessage(
        final List<ToolCallAccumulator.CompletedToolCall> callsToRun
    ) {
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
                type    : call.toolType ?: 'function',
                function: [
                    index    : call.index ?: Integer.valueOf(i),
                    name     : call.functionName,
                    arguments: call.argumentsJson ?: Collections.<String, Object>emptyMap()
                ]
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
        sink.next(new ProviderChunk('provider_message', messageId, null, [
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

    private static List<Map<String, Object>> filterTools(
        final List<Map<String, Object>> tools,
        final Set<String> excludedToolNames
    ) {
        if (tools == null || tools.isEmpty()) {
            return Collections.<Map<String, Object>>emptyList()
        }
        if (excludedToolNames == null || excludedToolNames.isEmpty()) {
            return tools
        }

        final List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>(tools.size())
        for (int i = 0; i < tools.size(); i++) {
            final Map<String, Object> tool = tools.get(i)
            final String name = toolName(tool)
            if (name == null || !excludedToolNames.contains(name)) {
                filtered.add(tool)
            }
        }
        return filtered
    }

    private static Set<String> toolNamesFromTools(final List<Map<String, Object>> tools) {
        final Set<String> names = new LinkedHashSet<String>()
        if (tools == null) {
            return names
        }
        for (int i = 0; i < tools.size(); i++) {
            final String name = toolName(tools.get(i))
            if (name != null && !name.trim().isEmpty()) {
                names.add(name)
            }
        }
        return names
    }

    private static String toolName(final Map<String, Object> tool) {
        if (tool == null) {
            return null
        }
        final Object functionObj = tool.get('function')
        if (functionObj instanceof Map) {
            @SuppressWarnings('unchecked')
            final Map<String, Object> function = (Map<String, Object>) functionObj
            final Object value = function.get('name')
            return value == null ? null : String.valueOf(value)
        }
        final Object value = tool.get('name')
        return value == null ? null : String.valueOf(value)
    }

    private static String toolCallKey(final ToolCallAccumulator.CompletedToolCall call) {
        if (call == null) {
            return ''
        }
        final Map<String, Object> args = call.argumentsJson == null
            ? Collections.<String, Object>emptyMap()
            : call.argumentsJson
        return (call.functionName ?: '') + ':' + JsonOutput.toJson(args)
    }

    private String extractLatestUserPrompt(final List<ProviderMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            final ProviderMessage message = messages.get(i)
            if ('user'.equals(message.role)) {
                return message.content
            }
        }
        return null
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
    private static final class ToolExecResult {
        final boolean executed
        final boolean error

        ToolExecResult(final boolean executed, final boolean error) {
            this.executed = executed
            this.error = error
        }

        static ToolExecResult executed() { new ToolExecResult(true, false) }
        static ToolExecResult error() { new ToolExecResult(false, true) }
    }

    @CompileStatic
    private static final class HeaderFilterState {
        boolean inHeader = false
    }

    @CompileStatic
    private static final class ThinkingState {
        boolean inThinking = false
        boolean explicitThinkingMode = false
        boolean forceThinkingUntilTagEnd = false
        final StringBuilder carry = new StringBuilder(16)
    }

    @CompileStatic
    private static final class OllamaTurnResult {
        final List<ToolCallAccumulator.CompletedToolCall> toolCalls
        final List<Map<String, Object>> malformedStructuredCalls
        final String assistantText

        OllamaTurnResult(
            final List<ToolCallAccumulator.CompletedToolCall> toolCalls,
            final List<Map<String, Object>> malformedStructuredCalls,
            final String assistantText
        ) {
            this.toolCalls = toolCalls
            this.malformedStructuredCalls = malformedStructuredCalls
            this.assistantText = assistantText
        }
    }
}
