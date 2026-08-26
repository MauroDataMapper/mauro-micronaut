package org.maurodata.service.chat.llm.langchain4j

import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.CompleteToolCall
import dev.langchain4j.model.chat.response.PartialResponse
import dev.langchain4j.model.chat.response.PartialResponseContext
import dev.langchain4j.model.chat.response.PartialThinking
import dev.langchain4j.model.chat.response.PartialThinkingContext
import dev.langchain4j.model.chat.response.PartialToolCall
import dev.langchain4j.model.chat.response.PartialToolCallContext
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.maurodata.plugin.chat.api.chat.ToolInvokeRequest
import org.maurodata.plugin.chat.api.chat.ToolInvokeResponse
import org.maurodata.service.chat.ChatMcpService
import org.maurodata.service.chat.llm.LlmProvider
import org.maurodata.service.chat.llm.ProviderChunk
import org.maurodata.service.chat.llm.ProviderError
import org.maurodata.service.chat.llm.ProviderErrorClassifier
import org.maurodata.service.chat.llm.ProviderRequest
import org.maurodata.service.chat.llm.config.ChatProviderConfiguration
import org.maurodata.service.chat.llm.config.ChatProviderConfigurationResolver
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@CompileStatic
abstract class LangChain4jStreamingProvider implements LlmProvider {

    private final String providerId
    private final ChatProviderConfigurationResolver configurationResolver
    private final ChatMcpService mcpService
    private final LangChain4jModelFactory modelFactory = new LangChain4jModelFactory()
    private final LangChain4jRequestMapper mapper = new LangChain4jRequestMapper()
    private final JsonSlurper slurper = new JsonSlurper()

    protected LangChain4jStreamingProvider(String providerId, ChatProviderConfigurationResolver configurationResolver, ChatMcpService mcpService) {
        this.providerId = providerId
        this.configurationResolver = configurationResolver
        this.mcpService = mcpService
    }

    @Override
    String id() {
        providerId
    }

    @Override
    Publisher<ProviderChunk> streamChat(final ProviderRequest request) {
        Flux.create({FluxSink<ProviderChunk> sink ->
            Thread thread = new Thread(new Runnable() {
                @Override
                void run() {
                    try {
                        ChatProviderConfiguration configuration = configurationResolver.resolve(providerId)
                        if (!validateConfiguration(configuration, request, sink)) {
                            return
                        }
                        ProviderRequest effectiveRequest = withDefaultParameters(request, configuration)
                        StreamingChatModel model = modelFactory.createStreamingModel(configuration, effectiveRequest.model)
                        List<ChatMessage> workingMessages = mapper.toChatMessages(effectiveRequest.messages)
                        boolean disableToolLoop = Boolean.TRUE.equals(effectiveRequest.options?.get('_mauroDisableToolLoop'))

                        boolean continueLoop = true
                        while (continueLoop) {
                            TurnResult turn = streamOneTurn(model, effectiveRequest, workingMessages, sink)
                            if (turn.toolRequests.isEmpty()) {
                                continueLoop = false
                            } else if (disableToolLoop) {
                                for (ToolExecutionRequest toolRequest : turn.toolRequests) {
                                    emitToolCall(toolRequest, effectiveRequest, sink)
                                }
                                continueLoop = false
                            } else {
                                ChatMessage aiMessage = mapper.toAiMessage(turn.toolRequests, turn.visibleText.toString())
                                workingMessages.add(aiMessage)
                                emitProviderMessage(sink, effectiveRequest.messageId, aiMessage)
                                for (ToolExecutionRequest toolRequest : turn.toolRequests) {
                                    handleToolCall(toolRequest, effectiveRequest, workingMessages, sink)
                                }
                            }
                        }
                    } catch (Throwable t) {
                        ProviderError error = ProviderErrorClassifier.classify(providerId, t)
                        sink.next(new ProviderChunk('error', request.messageId, error.message ?: t.message ?: t.class.simpleName, errorMetadata(error), error))
                    } finally {
                        sink.complete()
                    }
                }
            }, providerId + '-langchain4j-stream-' + request.sessionId)
            thread.daemon = true
            thread.start()
        })
    }

    private boolean validateConfiguration(ChatProviderConfiguration configuration, ProviderRequest request, FluxSink<ProviderChunk> sink) {
        if (request.model == null || request.model.trim().isEmpty()) {
            ProviderError error = ProviderErrorClassifier.configuration(providerId, "Missing model for ${providerId} request".toString())
            sink.next(new ProviderChunk('error', request.messageId, error.message, errorMetadata(error), error))
            return false
        }
        if ('openai'.equalsIgnoreCase(configuration.effectiveType()) && !configuration.hasApiKey()) {
            ProviderError error = ProviderErrorClassifier.configuration(providerId, 'Missing OpenAI API key')
            sink.next(new ProviderChunk('error', request.messageId, error.message, errorMetadata(error), error))
            return false
        }
        true
    }

    private static ProviderRequest withDefaultParameters(ProviderRequest request, ChatProviderConfiguration configuration) {
        Map<String, Object> options = new LinkedHashMap<String, Object>()
        options.putAll(configuration.defaultParameters ?: Collections.<String, Object>emptyMap())
        options.putAll(request.options ?: Collections.<String, Object>emptyMap())
        options = LangChain4jRequestOptionsPolicy.sanitize(configuration.effectiveType(), request.model, options)
        new ProviderRequest(
            sessionId: request.sessionId,
            messageId: request.messageId,
            model: request.model,
            messages: request.messages,
            tools: request.tools,
            options: options
        )
    }

    private TurnResult streamOneTurn(
        StreamingChatModel model,
        ProviderRequest providerRequest,
        List<ChatMessage> workingMessages,
        FluxSink<ProviderChunk> sink
    ) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1)
        TurnResult result = new TurnResult()
        ChatRequest chatRequest = mapper.toChatRequest(providerRequest, workingMessages)

        model.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            void onPartialResponse(String partialResponse) {
                if (partialResponse != null && !partialResponse.isEmpty()) {
                    result.visibleText.append(partialResponse)
                    sink.next(new ProviderChunk('token', providerRequest.messageId, partialResponse, Collections.<String, Object>emptyMap()))
                }
            }

            @Override
            void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                onPartialResponse(partialResponse?.text())
            }

            @Override
            void onPartialThinking(PartialThinking partialThinking) {
                String text = partialThinking?.text()
                if (text != null && !text.isEmpty()) {
                    if (!result.thinkingStarted) {
                        result.thinkingStarted = true
                        sink.next(new ProviderChunk('thinking_start', providerRequest.messageId, null, Collections.<String, Object>emptyMap()))
                    }
                    sink.next(new ProviderChunk('thinking_token', providerRequest.messageId, text, Collections.<String, Object>emptyMap()))
                }
            }

            @Override
            void onPartialThinking(PartialThinking partialThinking, PartialThinkingContext context) {
                onPartialThinking(partialThinking)
            }

            @Override
            void onPartialToolCall(PartialToolCall partialToolCall) {
                // Keep Mauro's public stream stable: emit only complete tool_call events.
            }

            @Override
            void onPartialToolCall(PartialToolCall partialToolCall, PartialToolCallContext context) {
                onPartialToolCall(partialToolCall)
            }

            @Override
            void onCompleteToolCall(CompleteToolCall completeToolCall) {
                if (completeToolCall != null && completeToolCall.toolExecutionRequest() != null) {
                    result.toolRequests.add(completeToolCall.toolExecutionRequest())
                }
            }

            @Override
            void onCompleteResponse(ChatResponse completeResponse) {
                if (result.thinkingStarted) {
                    sink.next(new ProviderChunk('thinking_end', providerRequest.messageId, null, Collections.<String, Object>emptyMap()))
                }
                latch.countDown()
            }

            @Override
            void onError(Throwable error) {
                ProviderError providerError = ProviderErrorClassifier.classify(providerId, error)
                sink.next(new ProviderChunk('error', providerRequest.messageId, providerError.message ?: error?.message ?: 'LangChain4j provider error', errorMetadata(providerError), providerError))
                latch.countDown()
            }
        })

        latch.await(timeoutSeconds(providerRequest), TimeUnit.SECONDS)
        result
    }

    private void handleToolCall(
        ToolExecutionRequest toolRequest,
        ProviderRequest providerRequest,
        List<ChatMessage> workingMessages,
        FluxSink<ProviderChunk> sink
    ) {
        Map<String, Object> arguments = argumentsMap(toolRequest.arguments())
        emitToolCall(toolRequest, providerRequest, sink)

        ToolInvokeResponse invokeResponse = mcpService.invokeTool(toolRequest.name(), new ToolInvokeRequest(
            arguments: arguments,
            forwardHeaders: forwardedHeadersFromOptions(providerRequest.options)
        ))

        String invocationId = extractInvocationId(invokeResponse)
        Map<String, Object> resultMeta = [
            callId: toolRequest.id(),
            ok    : invokeResponse.success,
            output: invokeResponse.result
        ] as Map<String, Object>
        if (invocationId != null) {
            resultMeta.put('invocationId', invocationId)
        }
        if (invokeResponse.error != null) {
            resultMeta.put('error', invokeResponse.error)
        }
        sink.next(new ProviderChunk('tool_result', providerRequest.messageId, null, resultMeta))

        String fallbackJson = JsonOutput.toJson([
            callId      : toolRequest.id(),
            invocationId: invocationId,
            ok          : invokeResponse.success,
            output      : invokeResponse.result,
            error       : invokeResponse.error
        ])
        ChatMessage toolMessage = mapper.toToolResultMessage(
            toolRequest.id(),
            toolRequest.name(),
            invokeResponse.modelText ?: fallbackJson,
            !Boolean.TRUE.equals(invokeResponse.success)
        )
        workingMessages.add(toolMessage)
        emitProviderMessage(sink, providerRequest.messageId, toolMessage)
    }

    private void emitToolCall(ToolExecutionRequest toolRequest, ProviderRequest providerRequest, FluxSink<ProviderChunk> sink) {
        sink.next(new ProviderChunk('tool_call', providerRequest.messageId, null, [
            callId   : toolRequest.id(),
            name     : toolRequest.name(),
            arguments: argumentsMap(toolRequest.arguments())
        ] as Map<String, Object>))
    }

    private Map<String, Object> argumentsMap(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.trim().isEmpty()) {
            return Collections.<String, Object>emptyMap()
        }
        Object parsed = slurper.parseText(argumentsJson)
        parsed instanceof Map ? new LinkedHashMap<String, Object>((Map<String, Object>) parsed) : Collections.<String, Object>emptyMap()
    }

    private static long timeoutSeconds(ProviderRequest providerRequest) {
        Object value = providerRequest.options?.get('timeoutSeconds')
        value instanceof Number ? Math.max(1L, ((Number) value).longValue()) : 300L
    }

    private static Map<String, List<String>> forwardedHeadersFromOptions(final Map<String, Object> options) {
        if (options == null || !(options.get('_mauroForwardHeaders') instanceof Map)) {
            return Collections.<String, List<String>>emptyMap()
        }
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>()
        ((Map<?, ?>) options.get('_mauroForwardHeaders')).each {Object key, Object value ->
            if (key != null && value instanceof Collection) {
                List<String> values = ((Collection<?>) value).collect {Object item -> String.valueOf(item)}.findAll {String item -> item != null && !item.trim().isEmpty()} as List<String>
                if (!values.isEmpty()) {
                    headers.put(String.valueOf(key), values)
                }
            }
        }
        headers
    }

    private static String extractInvocationId(final ToolInvokeResponse invokeResponse) {
        final Object result = invokeResponse?.result
        if (result instanceof Map) {
            final Object invocationId = ((Map<?, ?>) result).get('invocationId')
            if (invocationId != null) {
                return String.valueOf(invocationId)
            }
        }
        null
    }

    private void emitProviderMessage(FluxSink<ProviderChunk> sink, String messageId, ChatMessage message) {
        sink.next(new ProviderChunk('provider_request_message', messageId, null, [
            providerMessage: mapper.toProviderMessageMap(message)
        ] as Map<String, Object>))
    }

    private static Map<String, Object> errorMetadata(ProviderError error) {
        if (error == null) {
            return Collections.<String, Object>emptyMap()
        }
        [error: error.toMetadata()] as Map<String, Object>
    }

    private static class TurnResult {
        final List<ToolExecutionRequest> toolRequests = new ArrayList<ToolExecutionRequest>()
        final StringBuilder visibleText = new StringBuilder()
        boolean thinkingStarted = false
    }
}
