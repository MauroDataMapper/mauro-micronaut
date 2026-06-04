package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
final class OllamaToolingPolicy {

    private static final Set<String> TOOL_INTENT_MARKERS = new LinkedHashSet<String>(Arrays.asList(
        'use tool',
        'use the',
        'invoke',
        'call tool',
        'run tool',
        'mcp',
        'function call'
    ))

    private OllamaToolingPolicy() {
    }

    static boolean isToolIntent(final String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return false
        }
        final String lower = userPrompt.toLowerCase(Locale.ROOT)
        for (String marker : TOOL_INTENT_MARKERS) {
            if (lower.contains(marker)) {
                return true
            }
        }
        return false
    }

    static boolean shouldPublishMalformedWarning(final boolean toolIntent, final boolean toolsEnabled) {
        return toolsEnabled && toolIntent
    }

    static boolean isControlToken(final String token) {
        if (token == null) return false
        final String trimmed = token.trim()
        if (trimmed.isEmpty()) return false
        return '<|start_header_id|>'.equals(trimmed)
            || '<|end_header_id|>'.equals(trimmed)
            || '<|eot_id|>'.equals(trimmed)
    }

    static boolean shouldRunFallbackExtractor(final int validStructuredToolCalls) {
        return validStructuredToolCalls == 0
    }
}
