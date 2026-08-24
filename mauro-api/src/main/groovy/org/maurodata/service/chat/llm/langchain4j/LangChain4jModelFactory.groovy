package org.maurodata.service.chat.llm.langchain4j

import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import groovy.transform.CompileStatic
import org.maurodata.service.chat.llm.config.ChatProviderConfiguration

import java.time.Duration

@CompileStatic
class LangChain4jModelFactory {

    StreamingChatModel createStreamingModel(ChatProviderConfiguration configuration, String modelName) {
        final String type = configuration.effectiveType()?.toLowerCase(Locale.ROOT)
        if ('openai'.equals(type) || 'open-ai'.equals(type) || 'openai-compatible'.equals(type)) {
            return OpenAiStreamingChatModel.builder()
                .baseUrl(configuration.baseUrl ?: 'https://api.openai.com/v1')
                .apiKey(configuration.apiKey ?: '')
                .modelName(modelName)
                .timeout(Duration.ofSeconds(Math.max(1, configuration.timeoutSeconds ?: 120)))
                .logRequests(configuration.logRequests)
                .logResponses(configuration.logResponses)
                .build()
        }
        if ('ollama'.equals(type)) {
            return (StreamingChatModel) OllamaStreamingChatModel.builder()
                .baseUrl(configuration.baseUrl ?: 'http://localhost:11434')
                .modelName(modelName)
                .timeout(Duration.ofSeconds(Math.max(1, configuration.timeoutSeconds ?: 120)))
                .logRequests(configuration.logRequests)
                .logResponses(configuration.logResponses)
                .build()
        }
        throw new IllegalArgumentException("Unsupported LangChain4j provider type: ${configuration.effectiveType()}")
    }
}
