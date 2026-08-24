package org.maurodata.plugin.chat.providers

import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import org.maurodata.service.chat.ChatMcpService
import org.maurodata.service.chat.llm.config.ChatProviderConfigurationResolver
import org.maurodata.service.chat.llm.langchain4j.LangChain4jStreamingProvider

@CompileStatic
@Singleton
class OpenAiProvider extends LangChain4jStreamingProvider {

    OpenAiProvider(ChatProviderConfigurationResolver configurationResolver, ChatMcpService mcpService) {
        super('openai', configurationResolver, mcpService)
    }
}
