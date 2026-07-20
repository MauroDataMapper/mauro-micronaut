package org.maurodata.service.chat

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class ChatPromptResourceService {

    static final String TOOL_POLICY = 'tool-policy'

    private final ChatPromptAssetService promptAssetService

    ChatPromptResourceService() {
        this(new ChatPromptAssetRegistryService(new ChatPromptAssetDefinitionLoader()))
    }

    ChatPromptResourceService(ChatPromptAssetService promptAssetService) {
        this.promptAssetService = promptAssetService
    }

    String getPrompt(String name) {
        ChatPromptAssetDefinition asset = promptAssetService?.findAsset(name)
        String prompt = asset?.instruction
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown chat prompt: ${name}")
        }
        prompt.trim()
    }
}
