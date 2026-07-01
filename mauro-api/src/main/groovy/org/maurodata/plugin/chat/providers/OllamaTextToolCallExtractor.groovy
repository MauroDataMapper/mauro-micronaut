package org.maurodata.plugin.chat.providers

import org.maurodata.service.chat.capabilities.*
import org.maurodata.service.chat.llm.*

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
final class OllamaTextToolCallExtractor {

    @CompileStatic
    static final class ExtractedToolCall {
        final String name
        final Map<String, Object> parameters
        final String rawJson

        ExtractedToolCall(final String name, final Map<String, Object> parameters, final String rawJson) {
            this.name = name
            this.parameters = parameters
            this.rawJson = rawJson
        }
    }

    private final JsonSlurper slurper = new JsonSlurper()

    ExtractedToolCall extract(
        final String text,
        final Set<String> allowedToolNames
    ) {
        if (text == null || text.trim().isEmpty()) {
            return null
        }

        final JsonCandidate candidate = extractJsonCandidate(text)
        if (candidate == null) {
            return null
        }

        final Object parsed
        try {
            parsed = slurper.parseText(candidate.json)
        } catch (Throwable ignored) {
            return null
        }
        if (!(parsed instanceof Map)) {
            return null
        }

        @SuppressWarnings('unchecked')
        final Map<String, Object> root = (Map<String, Object>) parsed

        final ExtractedToolCall prefixed = fromPrefixedToolName(candidate.toolName, root, candidate.json, allowedToolNames)
        if (prefixed != null) {
            return prefixed
        }

        final ExtractedToolCall shapeA = fromNameParameters(root, candidate.json, allowedToolNames)
        if (shapeA != null) {
            return shapeA
        }

        final ExtractedToolCall shapeB = fromToolInput(root, candidate.json, allowedToolNames)
        if (shapeB != null) {
            return shapeB
        }

        final ExtractedToolCall shapeC = fromSingleToolKey(root, candidate.json, allowedToolNames)
        if (shapeC != null) {
            return shapeC
        }

        return null
    }

    static boolean looksLikeTextToolCall(final String text) {
        if (text == null || text.trim().isEmpty()) {
            return false
        }
        final String trimmed = text.trim()
        return trimmed.contains('[TOOL_CALLS]') || trimmed.toLowerCase(Locale.ROOT).contains('[tool_calls]')
    }

    private ExtractedToolCall fromPrefixedToolName(
        final String name,
        final Map<String, Object> root,
        final String rawJson,
        final Set<String> allowedToolNames
    ) {
        if (isBlank(name) || !isAllowed(name, allowedToolNames)) {
            return null
        }
        final Map<String, Object> params = coerceParams(root, name)
        if (params == null) {
            return null
        }
        return new ExtractedToolCall(name, params, rawJson)
    }

    private ExtractedToolCall fromNameParameters(
        final Map<String, Object> root,
        final String rawJson,
        final Set<String> allowedToolNames
    ) {
        final String name = asString(root.get('name'))
        if (isBlank(name) || !isAllowed(name, allowedToolNames)) {
            return null
        }

        final Object parametersObj = root.get('parameters')
        final Map<String, Object> params = coerceParams(parametersObj, name)
        if (params == null) {
            return null
        }

        return new ExtractedToolCall(name, params, rawJson)
    }

    private ExtractedToolCall fromToolInput(
        final Map<String, Object> root,
        final String rawJson,
        final Set<String> allowedToolNames
    ) {
        final String name = asString(root.get('tool'))
        if (isBlank(name) || !isAllowed(name, allowedToolNames)) {
            return null
        }

        Object inputObj = root.get('input')
        if (inputObj == null) {
            inputObj = root.get('arguments')
        }
        final Map<String, Object> params = coerceParams(inputObj, name)
        if (params == null) {
            return null
        }

        return new ExtractedToolCall(name, params, rawJson)
    }

    private ExtractedToolCall fromSingleToolKey(
        final Map<String, Object> root,
        final String rawJson,
        final Set<String> allowedToolNames
    ) {
        if (root.size() != 1) {
            return null
        }

        final String onlyKey = root.keySet().iterator().next()
        if (isBlank(onlyKey) || !isAllowed(onlyKey, allowedToolNames)) {
            return null
        }

        final Object value = root.get(onlyKey)
        final Map<String, Object> params = coerceParams(value, onlyKey)
        if (params == null) {
            return null
        }

        return new ExtractedToolCall(onlyKey, params, rawJson)
    }

    private Map<String, Object> coerceParams(final Object obj, final String toolName) {
        Map<String, Object> params
        if (obj == null) {
            params = new LinkedHashMap<String, Object>()
        } else if (obj instanceof Map) {
            @SuppressWarnings('unchecked')
            final Map<String, Object> cast = (Map<String, Object>) obj
            params = new LinkedHashMap<String, Object>(cast)
        } else {
            return null
        }
        return params
    }

    private boolean isAllowed(final String toolName, final Set<String> allowedToolNames) {
        if (allowedToolNames == null || allowedToolNames.isEmpty()) {
            return false
        }
        return allowedToolNames.contains(toolName)
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty()
    }

    private JsonCandidate extractJsonCandidate(final String text) {
        final String trimmed = text.trim()

        final int fenceStart = trimmed.indexOf('```')
        if (fenceStart >= 0) {
            final int firstLineEnd = trimmed.indexOf('\n', fenceStart)
            if (firstLineEnd > fenceStart) {
                final int fenceEnd = trimmed.indexOf('```', firstLineEnd + 1)
                if (fenceEnd > firstLineEnd) {
                    final String inside = trimmed.substring(firstLineEnd + 1, fenceEnd).trim()
                    final JsonCandidate byBraces = sliceFirstBalancedObject(inside)
                    if (byBraces != null) {
                        return byBraces
                    }
                }
            }
        }

        return sliceFirstBalancedObject(trimmed)
    }

    private JsonCandidate sliceFirstBalancedObject(final String text) {
        final int start = text.indexOf('{')
        if (start < 0) {
            return null
        }

        int depth = 0
        boolean inString = false
        boolean escaped = false

        for (int i = start; i < text.length(); i++) {
            final char ch = text.charAt(i)

            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }

            if (ch == '"') {
                inString = true
                continue
            }

            if (ch == '{') {
                depth++
            } else if (ch == '}') {
                depth--
                if (depth == 0) {
                    return new JsonCandidate(text.substring(start, i + 1), extractPrefixedToolName(text.substring(0, start)))
                }
            }
        }

        return null
    }

    private static String extractPrefixedToolName(final String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return null
        }
        String cleaned = prefix.trim()
        final int marker = cleaned.lastIndexOf('[TOOL_CALLS]')
        if (marker >= 0) {
            cleaned = cleaned.substring(marker + '[TOOL_CALLS]'.length()).trim()
        }
        final int lowerMarker = cleaned.toLowerCase(Locale.ROOT).lastIndexOf('[tool_calls]')
        if (lowerMarker >= 0) {
            cleaned = cleaned.substring(lowerMarker + '[tool_calls]'.length()).trim()
        }
        int end = cleaned.length()
        while (end > 0) {
            final char ch = cleaned.charAt(end - 1)
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.') {
                break
            }
            end--
        }
        int start = end
        while (start > 0) {
            final char ch = cleaned.charAt(start - 1)
            if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.')) {
                break
            }
            start--
        }
        if (start < end) {
            return cleaned.substring(start, end)
        }
        return null
    }

    private static String asString(final Object value) {
        return value == null ? null : String.valueOf(value)
    }

    @CompileStatic
    private static final class JsonCandidate {
        final String json
        final String toolName

        JsonCandidate(final String json, final String toolName) {
            this.json = json
            this.toolName = toolName
        }
    }
}
