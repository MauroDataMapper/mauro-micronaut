package org.maurodata.service.chat.llm.langchain4j

import groovy.transform.CompileStatic

@CompileStatic
class LangChain4jRequestOptionsPolicy {

    static Map<String, Object> sanitize(String providerType, String modelName, Map<String, Object> options) {
        Map<String, Object> sanitized = new LinkedHashMap<String, Object>(options ?: Collections.<String, Object>emptyMap())
        if (openAiProvider(providerType) && defaultSamplingOnlyModel(modelName)) {
            sanitized.remove('temperature')
            sanitized.remove('topP')
            sanitized.remove('top_p')
        }
        sanitized
    }

    private static boolean openAiProvider(String providerType) {
        String normalized = providerType == null ? '' : providerType.toLowerCase(Locale.ROOT)
        normalized == 'openai' || normalized == 'open-ai'
    }

    private static boolean defaultSamplingOnlyModel(String modelName) {
        String normalized = modelName == null ? '' : modelName.toLowerCase(Locale.ROOT)
        normalized.startsWith('gpt-5') || normalized ==~ /o\d.*/ || normalized.startsWith('o4-') || normalized.startsWith('o3-') || normalized.startsWith('o1-')
    }
}
