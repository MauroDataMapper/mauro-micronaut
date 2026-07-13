package org.maurodata.plugin.chat.providers

import org.maurodata.service.chat.capabilities.*
import org.maurodata.service.chat.llm.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.ToolInvokeRequest
import org.maurodata.plugin.chat.api.chat.ToolInvokeResponse
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
    private static final Set<String> REUSABLE_TOOLS = new LinkedHashSet<String>(Arrays.asList('mauro_skill', 'mauro_get')).asImmutable()

    private final String baseUrl
    private final boolean toolsEnabled
    private final boolean defaultThink
    private final boolean traceWire
    private final boolean traceTurnDebug
    private final boolean traceThinking
    private final long thinkingCapabilityCacheSeconds
    private final int defaultNumCtx
    private final int maxNumCtx
    private final int defaultNumPredict
    private final int promptCharsPerToken
    private final int timeoutSeconds
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
        @Value('${chat.providers.ollama.default-num-ctx:8192}') final int defaultNumCtx,
        @Value('${chat.providers.ollama.max-num-ctx:32768}') final int maxNumCtx,
        @Value('${chat.providers.ollama.default-num-predict:2048}') final int defaultNumPredict,
        @Value('${chat.providers.ollama.prompt-chars-per-token:4}') final int promptCharsPerToken,
        @Value('${chat.providers.ollama.timeout-seconds:300}') final int timeoutSeconds,
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
        this.defaultNumCtx = Math.max(4096, defaultNumCtx)
        this.maxNumCtx = Math.max(this.defaultNumCtx, maxNumCtx)
        this.defaultNumPredict = Math.max(256, defaultNumPredict)
        this.promptCharsPerToken = Math.max(1, promptCharsPerToken)
        this.timeoutSeconds = Math.max(1, timeoutSeconds)
        this.mcpService = mcpService
        this.promptResourceService = promptResourceService
        LOG.info(
            'OLLAMA_CONFIG baseUrl={} toolsEnabled={} defaultThink={} traceWire={} traceTurnDebug={} traceThinking={} defaultNumCtx={} maxNumCtx={} defaultNumPredict={} promptCharsPerToken={} timeoutSeconds={}',
            this.baseUrl,
            Boolean.valueOf(this.toolsEnabled),
            Boolean.valueOf(this.defaultThink),
            Boolean.valueOf(this.traceWire),
            Boolean.valueOf(this.traceTurnDebug),
            Boolean.valueOf(this.traceThinking),
            Integer.valueOf(this.defaultNumCtx),
            Integer.valueOf(this.maxNumCtx),
            Integer.valueOf(this.defaultNumPredict),
            Integer.valueOf(this.promptCharsPerToken),
            Integer.valueOf(this.timeoutSeconds)
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
                        int emptyContinuationRetries = 0
                        int missingToolCallRetries = 0
                        final Set<String> executedToolNames = new LinkedHashSet<String>()
                        final Set<String> executedToolCallKeys = new LinkedHashSet<String>()
                        boolean hasToolResultContext = false
                        boolean finalAnswerOnly = false
                        boolean continueLoop = true
                        while (continueLoop) {
                            final TurnDebugAccumulator debug = new TurnDebugAccumulator()
                            final List<Map<String, Object>> availableTools = finalAnswerOnly
                                ? Collections.<Map<String, Object>>emptyList()
                                : filterTools(request.tools, executedToolNames)
                            final Set<String> allowedToolNames = toolNamesFromTools(availableTools)
                            final OllamaTurnResult turn = streamOneTurn(request, workingMessages, sink, toolIntent, debug, availableTools)
                            debug.structuredToolCalls = turn.toolCalls.size()

                            final List<ToolCallAccumulator.CompletedToolCall> executableCalls = new ArrayList<ToolCallAccumulator.CompletedToolCall>()
                            final List<ToolCallAccumulator.CompletedToolCall> blockedCalls = new ArrayList<ToolCallAccumulator.CompletedToolCall>()
                            for (int i = 0; i < turn.toolCalls.size(); i++) {
                                final ToolCallAccumulator.CompletedToolCall call = normalizeToolCall(turn.toolCalls.get(i), allowedToolNames)
                                final String fn = call.functionName
                                if (fn != null && !fn.trim().isEmpty() && allowedToolNames.contains(fn)) {
                                    executableCalls.add(call)
                                } else if (fn != null && !fn.trim().isEmpty()) {
                                    blockedCalls.add(call)
                                }
                            }
                            debug.executableStructuredToolCalls = executableCalls.size()

                            List<ToolCallAccumulator.CompletedToolCall> callsToRun = executableCalls
                            boolean fallbackSynthesized = false
                            if ((toolIntent || OllamaTextToolCallExtractor.looksLikeTextToolCall(turn.assistantText)) &&
                                OllamaToolingPolicy.shouldRunFallbackExtractor(executableCalls.size())) {
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
                            int blockedToolFeedbacks = 0

                            if (!callsToRun.isEmpty() || !blockedCalls.isEmpty()) {
                                toolRound++
                                List<ToolCallAccumulator.CompletedToolCall> acknowledgedCalls = new ArrayList<ToolCallAccumulator.CompletedToolCall>()
                                acknowledgedCalls.addAll(callsToRun)
                                acknowledgedCalls.addAll(blockedCalls)
                                ProviderMessage assistantToolCallMessage = buildAssistantToolCallMessage(
                                    acknowledgedCalls,
                                    visibleAssistantContentBeforeToolCall(turn.assistantText)
                                )
                                if (assistantToolCallMessage != null) {
                                    workingMessages.add(assistantToolCallMessage)
                                    emitProviderMessage(sink, request.messageId, assistantToolCallMessage)
                                }
                                for (int i = 0; i < callsToRun.size(); i++) {
                                    final ToolCallAccumulator.CompletedToolCall call = callsToRun.get(i)
                                    final String callKey = toolCallKey(call)
                                    if (executedToolCallKeys.contains(callKey)) {
                                        LOG.info('Ignored repeated tool call messageId={} toolName={} arguments={}', request.messageId, call.functionName, call.argumentsRaw)
                                        final String reason = repeatedToolCallReason(call)
                                        emitBlockedToolResult(call, reason, allowedToolNames, workingMessages, request.messageId, sink)
                                        blockedToolFeedbacks++
                                        continue
                                    }
                                    final ToolExecResult exec = handleToolCallStrict(
                                        call,
                                        workingMessages,
                                        request.messageId,
                                        sink,
                                        fallbackSynthesized,
                                        forwardedHeadersFromOptions(request.options)
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
                                for (int i = 0; i < blockedCalls.size(); i++) {
                                    final ToolCallAccumulator.CompletedToolCall call = blockedCalls.get(i)
                                    final String reason = blockedToolCallReason(call, request.tools, allowedToolNames, executedToolNames)
                                    LOG.info(
                                        'Blocked tool call messageId={} toolName={} arguments={} reason={}',
                                        request.messageId,
                                        call.functionName,
                                        call.argumentsRaw,
                                        reason
                                    )
                                    emitBlockedToolResult(call, reason, allowedToolNames, workingMessages, request.messageId, sink)
                                    blockedToolFeedbacks++
                                }
                            }
                            debug.assistantTextLength = turn.assistantText == null ? 0 : turn.assistantText.length()
                            if (traceTurnDebug) {
                                LOG.info('OLLAMA_TURN_DEBUG {}', debug.snapshot(request.sessionId, request.messageId))
                            }

                            if (shouldRetryPromisedToolCall(turn, allowedToolNames, callsToRun, blockedCalls, missingToolCallRetries)) {
                                missingToolCallRetries++
                                ProviderMessage toolCallNudge = new ProviderMessage(
                                    role: 'system',
                                    content: 'You said you would use an available tool, but did not emit a structured tool call. Continue now by emitting the appropriate tool_call with concrete arguments. If no tool is actually needed, provide the final answer directly. Do not ask the user for clarification when the current request already contains enough search terms.'
                                )
                                workingMessages.add(toolCallNudge)
                                emitProviderMessage(sink, request.messageId, toolCallNudge)
                                LOG.info('OLLAMA_MISSING_TOOL_CALL_RETRY sessionId={} messageId={} retry={}', request.sessionId, request.messageId, Integer.valueOf(missingToolCallRetries))
                                continue
                            }

                            if (shouldRetryEmptyToolContinuation(turn, hasToolResultContext, callsToRun, blockedCalls, emptyContinuationRetries)) {
                                emptyContinuationRetries++
                                ProviderMessage continuationNudge = new ProviderMessage(
                                    role: 'system',
                                    content: 'The previous tool result has been provided. Continue the user request now: either call the next required tool with concrete arguments, or provide the final answer from the available tool results. Do not return an empty message.'
                                )
                                workingMessages.add(continuationNudge)
                                emitProviderMessage(sink, request.messageId, continuationNudge)
                                LOG.info('OLLAMA_EMPTY_TOOL_CONTINUATION_RETRY sessionId={} messageId={} retry={}', request.sessionId, request.messageId, Integer.valueOf(emptyContinuationRetries))
                                continue
                            }

                            if ((successfulToolExecutions > 0 || blockedToolFeedbacks > 0) && toolRound >= ToolLoopGuards.MAX_TOOL_ROUNDS) {
                                hasToolResultContext = true
                                finalAnswerOnly = true
                                ProviderMessage finalAnswerNudge = new ProviderMessage(
                                    role: 'system',
                                    content: 'The maximum tool-call rounds for this response has been reached. Do not call more tools. Provide the final answer now using the tool results already available, and clearly mention any missing information.'
                                )
                                workingMessages.add(finalAnswerNudge)
                                emitProviderMessage(sink, request.messageId, finalAnswerNudge)
                                LOG.info('OLLAMA_FINAL_ANSWER_AFTER_MAX_TOOL_ROUNDS sessionId={} messageId={} toolRound={}', request.sessionId, request.messageId, Integer.valueOf(toolRound))
                                continue
                            }

                            continueLoop = ToolLoopGuards.shouldContinueToolLoop(
                                toolRound,
                                toolErrors,
                                successfulToolExecutions + blockedToolFeedbacks,
                                sink,
                                request.messageId
                            )
                            if (successfulToolExecutions > 0 || blockedToolFeedbacks > 0) {
                                hasToolResultContext = true
                            }
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
        final TextToolCallSuppressionState textToolCallSuppressionState = new TextToolCallSuppressionState()
        final OllamaResponseDiagnostics responseDiagnostics = new OllamaResponseDiagnostics()
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
            .timeout(Duration.ofSeconds(timeoutSeconds))
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
                    line, request.messageId, sink, accumulator, malformedStructuredCalls, assistantTextBuffer, toolIntent, debug, headerFilterState, thinkingState, textToolCallSuppressionState, responseDiagnostics
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

        if ('length'.equals(responseDiagnostics.doneReason)) {
            final String message = "Ollama stopped because the context/output limit was reached. prompt_eval_count=${responseDiagnostics.promptEvalCount ?: 'unknown'}, eval_count=${responseDiagnostics.evalCount ?: 'unknown'}."
            LOG.warn('OLLAMA_LENGTH_STOP sessionId={} messageId={} model={} {}', request.sessionId, request.messageId, request.model, message)
            sink.next(new ProviderChunk('error', request.messageId, message, [
                doneReason     : responseDiagnostics.doneReason,
                promptEvalCount: responseDiagnostics.promptEvalCount,
                evalCount      : responseDiagnostics.evalCount
            ] as Map<String, Object>))
        }

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
        final ThinkingState thinkingState,
        final TextToolCallSuppressionState textToolCallSuppressionState,
        final OllamaResponseDiagnostics responseDiagnostics
    ) {
        if (line == null || line.trim().isEmpty()) {
            return
        }

        final Object parsed = slurper.parseText(line)
        if (!(parsed instanceof Map)) {
            return
        }
        final Map<?, ?> root = (Map<?, ?>) parsed
        captureResponseDiagnostics(root, responseDiagnostics)
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
            routeThinkingAwareContent(content, messageId, sink, assistantTextBuffer, debug, headerFilterState, thinkingState, textToolCallSuppressionState)
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
        final boolean fallbackSynthesized,
        final Map<String, List<String>> forwardHeaders
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

            final ToolInvokeRequest invokeRequest = new ToolInvokeRequest(
                arguments: call.argumentsJson,
                forwardHeaders: forwardHeaders ?: Collections.<String, List<String>>emptyMap()
            )
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
            ProviderMessage toolMessage = new ProviderMessage('tool', toolResultText, call.callId, toolName)
            workingMessages.add(toolMessage)
            emitProviderMessage(sink, messageId, toolMessage)
            return ToolExecResult.executed()
        } catch (final Throwable t) {
            sink.next(new ProviderChunk('error', messageId, 'tool invocation failed: ' + t.getMessage(), Collections.<String, Object>emptyMap()))
            return ToolExecResult.error()
        }
    }

    private void emitBlockedToolResult(
        final ToolCallAccumulator.CompletedToolCall call,
        final String reason,
        final Set<String> allowedToolNames,
        final List<ProviderMessage> workingMessages,
        final String messageId,
        final reactor.core.publisher.FluxSink<ProviderChunk> sink
    ) {
        final String toolName = call.functionName ?: 'unknown'
        final String error = reason ?: 'Tool call was blocked by provider policy.'
        final Map<String, Object> resultMeta = new LinkedHashMap<String, Object>(5)
        resultMeta.put('callId', call.callId)
        resultMeta.put('ok', Boolean.FALSE)
        resultMeta.put('error', error)
        resultMeta.put('blocked', Boolean.TRUE)
        resultMeta.put('arguments', call.argumentsJson ?: Collections.<String, Object>emptyMap())
        resultMeta.put('availableTools', allowedToolNames == null ? [] : new ArrayList<String>(allowedToolNames))
        sink.next(new ProviderChunk('tool_result', messageId, null, resultMeta))

        final String toolResultText = blockedToolResultText(toolName, error, allowedToolNames)
        ProviderMessage toolMessage = new ProviderMessage('tool', toolResultText, call.callId, toolName)
        workingMessages.add(toolMessage)
        emitProviderMessage(sink, messageId, toolMessage)
    }

    private String buildRequestBody(
        final ProviderRequest request,
        final List<ProviderMessage> messages,
        final boolean toolIntent,
        final boolean thinkEnabled,
        final List<Map<String, Object>> availableTools
    ) {
        final List<Map<String, Object>> wireMessages = ProviderWireMessages.toWireMessages(messages, true)
        final Map<String, Object> ollamaOptions = buildOllamaOptions(request, wireMessages, availableTools)

        final Map<String, Object> body = new LinkedHashMap<String, Object>(5)
        body.put('model', request.model)
        body.put('stream', Boolean.TRUE)
        body.put('messages', wireMessages)
        if (toolsEnabled && availableTools != null && !availableTools.isEmpty()) {
            body.put('tools', availableTools)
        }
        if (request.options != null && !request.options.isEmpty()) {
            for (Map.Entry<String, Object> entry : request.options.entrySet()) {
                if (!'options'.equals(entry.key) && !'num_ctx'.equals(entry.key) && !'num_predict'.equals(entry.key) && !'_mauroForwardHeaders'.equals(entry.key)) {
                    body.put(entry.key, entry.value)
                }
            }
        }
        if (!ollamaOptions.isEmpty()) {
            body.put('options', ollamaOptions)
        }
        body.put('think', Boolean.valueOf(thinkEnabled))

        return JsonOutput.toJson(body)
    }

    private Map<String, Object> buildOllamaOptions(
        final ProviderRequest request,
        final List<Map<String, Object>> wireMessages,
        final List<Map<String, Object>> availableTools
    ) {
        final Map<String, Object> options = new LinkedHashMap<String, Object>()
        if (request.options != null) {
            final Object nestedOptions = request.options.get('options')
            if (nestedOptions instanceof Map) {
                @SuppressWarnings('unchecked')
                final Map<String, Object> typed = (Map<String, Object>) nestedOptions
                options.putAll(typed)
            }
            copyTopLevelOption(request.options, options, 'num_ctx')
            copyTopLevelOption(request.options, options, 'num_predict')
        }
        if (!options.containsKey('num_predict')) {
            options.put('num_predict', Integer.valueOf(defaultNumPredict))
        }
        if (!options.containsKey('num_ctx')) {
            final int estimatedPromptTokens = estimatePromptTokens(wireMessages, availableTools)
            final int numPredict = asPositiveInt(options.get('num_predict'), defaultNumPredict)
            final int desiredContext = nextPowerOfTwo(estimatedPromptTokens + numPredict + 512)
            options.put('num_ctx', Integer.valueOf(clamp(desiredContext, defaultNumCtx, maxNumCtx)))
        }
        options
    }

    private static void copyTopLevelOption(
        final Map<String, Object> requestOptions,
        final Map<String, Object> ollamaOptions,
        final String name
    ) {
        if (requestOptions.containsKey(name) && !ollamaOptions.containsKey(name)) {
            ollamaOptions.put(name, requestOptions.get(name))
        }
    }

    private static Map<String, List<String>> forwardedHeadersFromOptions(final Map<String, Object> options) {
        if (options == null) {
            return Collections.<String, List<String>>emptyMap()
        }
        final Object raw = options.get('_mauroForwardHeaders')
        if (!(raw instanceof Map)) {
            return Collections.<String, List<String>>emptyMap()
        }
        final Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>()
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (entry.key == null || !(entry.value instanceof Collection)) {
                continue
            }
            final List<String> values = new ArrayList<String>()
            for (Object value : (Collection<?>) entry.value) {
                if (value != null && !String.valueOf(value).trim().isEmpty()) {
                    values.add(String.valueOf(value))
                }
            }
            if (!values.isEmpty()) {
                headers.put(String.valueOf(entry.key), values)
            }
        }
        headers
    }

    private int estimatePromptTokens(final List<Map<String, Object>> wireMessages, final List<Map<String, Object>> availableTools) {
        int chars = JsonOutput.toJson(wireMessages ?: Collections.<Map<String, Object>>emptyList()).length()
        if (availableTools != null && !availableTools.isEmpty()) {
            chars += JsonOutput.toJson(availableTools).length()
        }
        Math.max(1, (int) Math.ceil(chars / (double) promptCharsPerToken))
    }

    private static int asPositiveInt(final Object value, final int fallback) {
        if (value instanceof Number) {
            final int parsed = ((Number) value).intValue()
            return parsed > 0 ? parsed : fallback
        }
        if (value != null) {
            try {
                final int parsed = Integer.parseInt(String.valueOf(value))
                return parsed > 0 ? parsed : fallback
            } catch (final NumberFormatException ignored) {
                return fallback
            }
        }
        fallback
    }

    private static int nextPowerOfTwo(final int value) {
        int out = 4096
        while (out < value && out < 65536) {
            out *= 2
        }
        out
    }

    private static int clamp(final int value, final int min, final int max) {
        Math.max(min, Math.min(max, value))
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

    private static void captureResponseDiagnostics(final Map<?, ?> root, final OllamaResponseDiagnostics diagnostics) {
        if (root == null || diagnostics == null) {
            return
        }
        final String doneReason = asString(root.get('done_reason'))
        if (doneReason != null && !doneReason.trim().isEmpty()) {
            diagnostics.doneReason = doneReason
        }
        diagnostics.promptEvalCount = asInteger(root.get('prompt_eval_count'), diagnostics.promptEvalCount)
        diagnostics.evalCount = asInteger(root.get('eval_count'), diagnostics.evalCount)
    }

    private static Integer asInteger(final Object value, final Integer fallback) {
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue())
        }
        if (value != null) {
            try {
                return Integer.valueOf(Integer.parseInt(String.valueOf(value)))
            } catch (final NumberFormatException ignored) {
                return fallback
            }
        }
        fallback
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
        final ThinkingState thinkingState,
        final TextToolCallSuppressionState textToolCallSuppressionState
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
                    emitVisibleSegment(remainder, messageId, sink, assistantTextBuffer, headerFilterState, textToolCallSuppressionState, debug)
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
                emitVisibleSegment(visible, messageId, sink, assistantTextBuffer, headerFilterState, textToolCallSuppressionState, debug)
                sink.next(new ProviderChunk('thinking_start', messageId, '', Collections.<String, Object>emptyMap()))
                thinkingState.inThinking = true
                thinkingState.explicitThinkingMode = false
                thinkingState.forceThinkingUntilTagEnd = false
                index = open + '<think>'.length()
                continue
            }

            final String tailHeld = holdTailForTag(input.substring(index), '<think>', thinkingState.carry)
            emitVisibleSegment(tailHeld, messageId, sink, assistantTextBuffer, headerFilterState, textToolCallSuppressionState, debug)
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
        final TextToolCallSuppressionState textToolCallSuppressionState,
        final TurnDebugAccumulator debug
    ) {
        if (segment == null || segment.isEmpty()) {
            return
        }
        if (textToolCallSuppressionState != null && textToolCallSuppressionState.suppressing) {
            assistantTextBuffer.append(segment)
            debug.filteredControlTokenChunks++
            return
        }
        if (isFilteredHeaderToken(segment, headerFilterState) || OllamaToolingPolicy.isControlToken(segment)) {
            debug.filteredControlTokenChunks++
            return
        }
        final int markerAt = textToolCallMarkerIndex(segment)
        if (markerAt >= 0) {
            final String beforeMarker = segment.substring(0, markerAt)
            final String markerAndAfter = segment.substring(markerAt)
            if (!beforeMarker.isEmpty()) {
                sink.next(new ProviderChunk('token', messageId, beforeMarker, Collections.<String, Object>emptyMap()))
                assistantTextBuffer.append(beforeMarker)
                debug.publishedTokenChunks++
            }
            assistantTextBuffer.append(markerAndAfter)
            if (textToolCallSuppressionState != null) {
                textToolCallSuppressionState.suppressing = true
            }
            debug.filteredControlTokenChunks++
            return
        }
        if (shouldSuppressTextToolCallSegment(assistantTextBuffer, segment)) {
            assistantTextBuffer.append(segment)
            if (textToolCallSuppressionState != null) {
                textToolCallSuppressionState.suppressing = true
            }
            debug.filteredControlTokenChunks++
            return
        }
        sink.next(new ProviderChunk('token', messageId, segment, Collections.<String, Object>emptyMap()))
        assistantTextBuffer.append(segment)
        debug.publishedTokenChunks++
    }

    private static boolean shouldSuppressTextToolCallSegment(final StringBuilder assistantTextBuffer, final String segment) {
        final String current = assistantTextBuffer == null ? '' : assistantTextBuffer.toString()
        final String combined = (current + (segment ?: '')).trim()
        if (combined.isEmpty()) {
            return false
        }
        if (OllamaTextToolCallExtractor.looksLikeTextToolCall(combined)) {
            return true
        }
        final String marker = '[TOOL_CALLS]'
        final String lowerMarker = '[tool_calls]'
        if (current.trim().isEmpty()) {
            final String trimmed = combined
            return marker.startsWith(trimmed) || lowerMarker.startsWith(trimmed.toLowerCase(Locale.ROOT))
        }
        return OllamaTextToolCallExtractor.looksLikeTextToolCall(current)
    }

    private static int textToolCallMarkerIndex(final String segment) {
        if (segment == null || segment.isEmpty()) {
            return -1
        }
        final int upper = segment.indexOf('[TOOL_CALLS]')
        final int lower = segment.toLowerCase(Locale.ROOT).indexOf('[tool_calls]')
        if (upper < 0) {
            return lower
        }
        if (lower < 0) {
            return upper
        }
        return Math.min(upper, lower)
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

    private static boolean shouldRetryEmptyToolContinuation(
        final OllamaTurnResult turn,
        final boolean hasToolResultContext,
        final List<ToolCallAccumulator.CompletedToolCall> callsToRun,
        final List<ToolCallAccumulator.CompletedToolCall> blockedCalls,
        final int emptyContinuationRetries
    ) {
        if (!hasToolResultContext || emptyContinuationRetries > 0) {
            return false
        }
        if (callsToRun != null && !callsToRun.isEmpty()) {
            return false
        }
        if (blockedCalls != null && !blockedCalls.isEmpty()) {
            return false
        }
        if (turn == null) {
            return false
        }
        if (turn.toolCalls != null && !turn.toolCalls.isEmpty()) {
            return false
        }
        final String assistantText = turn.assistantText
        assistantText == null || assistantText.trim().isEmpty()
    }

    private static boolean shouldRetryPromisedToolCall(
        final OllamaTurnResult turn,
        final Set<String> allowedToolNames,
        final List<ToolCallAccumulator.CompletedToolCall> callsToRun,
        final List<ToolCallAccumulator.CompletedToolCall> blockedCalls,
        final int missingToolCallRetries
    ) {
        if (missingToolCallRetries > 0 || allowedToolNames == null || allowedToolNames.isEmpty()) {
            return false
        }
        if (callsToRun != null && !callsToRun.isEmpty()) {
            return false
        }
        if (blockedCalls != null && !blockedCalls.isEmpty()) {
            return false
        }
        if (turn == null || (turn.toolCalls != null && !turn.toolCalls.isEmpty())) {
            return false
        }
        final String assistantText = turn.assistantText
        if (assistantText == null || assistantText.trim().isEmpty()) {
            return false
        }
        final String lower = assistantText.toLowerCase(Locale.ROOT)
        final boolean futureToolIntent =
            lower.contains('i will ') ||
                lower.contains("i'll ") ||
                lower.contains('let me ') ||
                lower.contains('i am going to ') ||
                lower.contains('i can now ') ||
                lower.contains('i need to ')
        if (!futureToolIntent) {
            return false
        }
        for (String toolName : allowedToolNames) {
            if (toolName != null && !toolName.trim().isEmpty() && lower.contains(toolName.toLowerCase(Locale.ROOT))) {
                return true
            }
        }
        false
    }

    private static ProviderMessage buildAssistantToolCallMessage(
        final List<ToolCallAccumulator.CompletedToolCall> callsToRun,
        final String assistantContent
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
            content: assistantContent ?: '',
            toolCalls: toolCalls
        )
    }

    private static String visibleAssistantContentBeforeToolCall(final String assistantText) {
        if (assistantText == null || assistantText.isEmpty()) {
            return ''
        }
        final int markerAt = textToolCallMarkerIndex(assistantText)
        if (markerAt < 0) {
            return assistantText
        }
        assistantText.substring(0, markerAt).trim()
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

    private static String blockedToolCallReason(
        final ToolCallAccumulator.CompletedToolCall call,
        final List<Map<String, Object>> allTools,
        final Set<String> allowedToolNames,
        final Set<String> excludedToolNames
    ) {
        final String toolName = call.functionName
        if (toolName == null || toolName.trim().isEmpty()) {
            return 'Tool call was missing a function name.'
        }
        if (allowedToolNames != null && allowedToolNames.contains(toolName)) {
            return 'Tool call was not executable.'
        }
        if (excludedToolNames != null && excludedToolNames.contains(toolName)) {
            return "Tool ${toolName} was already used in this assistant turn. Choose another available tool or answer using the results already returned."
        }
        final Set<String> allToolNames = toolNamesFromTools(allTools)
        if (!allToolNames.contains(toolName)) {
            return "Tool ${toolName} is not available. Choose one of the advertised tools."
        }
        return "Tool ${toolName} is not available for this continuation. Choose another available tool or answer using the results already returned."
    }

    private static String blockedToolResultText(
        final String toolName,
        final String error,
        final Set<String> allowedToolNames
    ) {
        final List<String> available = allowedToolNames == null ?
            Collections.<String>emptyList() :
            new ArrayList<String>(allowedToolNames).sort(false)
        final String availableText = available.isEmpty() ?
            'No tools are currently available in this continuation.' :
            'Currently available tools: ' + available.join(', ') + '.'
        "Tool ${toolName} was not executed: ${error}\n${availableText}\nRecover by calling one of the available tools with concrete arguments, or answer using the tool results already available."
    }

    private static ToolCallAccumulator.CompletedToolCall normalizeToolCall(
        final ToolCallAccumulator.CompletedToolCall call,
        final Set<String> allowedToolNames
    ) {
        if (call == null || call.functionName == null || allowedToolNames == null) {
            return call
        }
        if (allowedToolNames.contains(call.functionName)) {
            return call
        }
        if (!allowedToolNames.contains('mauro_skill')) {
            return call
        }
        final String skillId = skillIdFromPseudoToolName(call.functionName)
        if (skillId == null) {
            return call
        }
        final Map<String, Object> normalizedArgs = new LinkedHashMap<String, Object>(call.argumentsJson ?: Collections.<String, Object>emptyMap())
        if (!normalizedArgs.containsKey('id')) {
            normalizedArgs.put('id', skillId)
        }
        final String normalizedRaw = JsonOutput.toJson(normalizedArgs)
        return new ToolCallAccumulator.CompletedToolCall(
            call.index,
            call.callId,
            call.toolType,
            'mauro_skill',
            normalizedRaw,
            normalizedArgs
        )
    }

    private static String skillIdFromPseudoToolName(final String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return null
        }
        final String trimmed = toolName.trim()
        if (trimmed == 'mauro_skill') {
            return null
        }
        if (trimmed.startsWith('mauro_')) {
            return 'mauro-' + trimmed.substring('mauro_'.length()).replace('_', '-')
        }
        if (trimmed.startsWith('mauro-')) {
            return trimmed
        }
        null
    }

    private static String repeatedToolCallReason(final ToolCallAccumulator.CompletedToolCall call) {
        final String toolName = call?.functionName ?: 'unknown'
        final String args = call?.argumentsRaw
        if (args != null && !args.trim().isEmpty()) {
            return "The exact ${toolName} call with arguments ${args} has already been executed in this assistant turn. Use the existing result, call the next different tool/input needed, or provide the final answer."
        }
        "The exact ${toolName} call has already been executed in this assistant turn. Use the existing result, call the next different tool/input needed, or provide the final answer."
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
    private static final class TextToolCallSuppressionState {
        boolean suppressing = false
    }

    @CompileStatic
    private static final class OllamaResponseDiagnostics {
        String doneReason
        Integer promptEvalCount
        Integer evalCount
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
