package org.maurodata.service.chat.llm.config

import groovy.transform.CompileStatic
import io.micronaut.context.env.Environment
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class ChatProviderConfigurationResolver {

    private final Environment environment

    ChatProviderConfigurationResolver(Environment environment) {
        this.environment = environment
    }

    ChatProviderConfiguration resolve(String providerId) {
        final String prefix = "chat.providers.${providerId}"
        new ChatProviderConfiguration(
            id: providerId,
            type: stringProperty("${prefix}.type", providerId),
            baseUrl: stringProperty("${prefix}.base-url", defaultBaseUrl(providerId)),
            apiKey: stringProperty("${prefix}.api-key", ''),
            modelAllowlist: listProperty("${prefix}.model-allowlist"),
            timeoutSeconds: integerProperty("${prefix}.timeout-seconds", 120),
            logRequests: booleanProperty("${prefix}.log-requests", false),
            logResponses: booleanProperty("${prefix}.log-responses", false),
            defaultParameters: mapProperty("${prefix}.default-parameters")
        )
    }

    private String stringProperty(String key, String defaultValue) {
        environment.getProperty(key, String).orElse(defaultValue)
    }

    private Integer integerProperty(String key, Integer defaultValue) {
        environment.getProperty(key, Integer).orElse(defaultValue)
    }

    private Boolean booleanProperty(String key, Boolean defaultValue) {
        environment.getProperty(key, Boolean).orElse(defaultValue)
    }

    private List<String> listProperty(String key) {
        List<String> values = environment.getProperty(key, List).orElse(Collections.emptyList()) as List<String>
        values.findAll {String value -> value != null && !value.trim().isEmpty()} as List<String>
    }

    private Map<String, Object> mapProperty(String key) {
        Map<String, Object> values = environment.getProperty(key, Map).orElse(Collections.emptyMap()) as Map<String, Object>
        new LinkedHashMap<String, Object>(values)
    }

    private static String defaultBaseUrl(String providerId) {
        if ('openai'.equalsIgnoreCase(providerId)) {
            return 'https://api.openai.com/v1'
        }
        if ('ollama'.equalsIgnoreCase(providerId)) {
            return 'http://localhost:11434'
        }
        ''
    }
}
