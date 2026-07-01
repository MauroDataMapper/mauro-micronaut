package org.maurodata.service.chat

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

import java.nio.charset.StandardCharsets

@CompileStatic
@Singleton
class ChatPromptResourceService {

    static final String TOOL_POLICY = 'tool-policy'

    private static final Map<String, List<String>> RESOURCE_PATHS = [
        (TOOL_POLICY): [
            'META-INF/mauro/chat/prompts/tool-policy.txt',
            'chat/prompts/tool-policy.txt'
        ] as List<String>
    ].asImmutable() as Map<String, List<String>>

    private final Map<String, String> prompts

    ChatPromptResourceService() {
        Map<String, String> loaded = new LinkedHashMap<String, String>()
        for (Map.Entry<String, List<String>> entry : RESOURCE_PATHS.entrySet()) {
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

    private static String loadRequired(List<String> paths) {
        InputStream inputStream = null
        for (String path : paths) {
            inputStream = ChatPromptResourceService.classLoader.getResourceAsStream(path)
            if (inputStream != null) {
                break
            }
        }
        if (inputStream == null) {
            throw new IllegalStateException("Missing chat prompt resource: ${paths.join(', ')}")
        }
        try {
            return inputStream.getText(StandardCharsets.UTF_8.name()).trim()
        } finally {
            inputStream.close()
        }
    }
}
