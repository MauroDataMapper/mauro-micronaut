package org.maurodata.service.chat

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

import java.nio.charset.StandardCharsets

@CompileStatic
@Singleton
class ChatPromptResourceService {

    static final String TOOL_POLICY = 'tool-policy'

    private static final Map<String, String> RESOURCE_PATHS = [
        (TOOL_POLICY): 'chat/prompts/tool-policy.txt'
    ].asImmutable() as Map<String, String>

    private final Map<String, String> prompts

    ChatPromptResourceService() {
        Map<String, String> loaded = new LinkedHashMap<String, String>()
        for (Map.Entry<String, String> entry : RESOURCE_PATHS.entrySet()) {
            loaded.put(entry.key, loadRequired(entry.value))
        }
        this.prompts = loaded.asImmutable() as Map<String, String>
    }

    String getPrompt(String name) {
        String prompt = prompts.get(name)
        if (prompt == null) {
            throw new IllegalArgumentException("Unknown chat prompt: ${name}")
        }
        prompt
    }

    private static String loadRequired(String path) {
        InputStream inputStream = ChatPromptResourceService.classLoader.getResourceAsStream(path)
        if (inputStream == null) {
            throw new IllegalStateException("Missing chat prompt resource: ${path}")
        }
        try {
            return inputStream.getText(StandardCharsets.UTF_8.name()).trim()
        } finally {
            inputStream.close()
        }
    }
}
