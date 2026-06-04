package org.maurodata.service.chat.llm

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

        final String candidate = extractJsonCandidate(text)
        if (candidate == null) {
            return null
        }

        final Object parsed
        try {
            parsed = slurper.parseText(candidate)
        } catch (Throwable ignored) {
            return null
        }
        if (!(parsed instanceof Map)) {
            return null
        }

        @SuppressWarnings('unchecked')
        final Map<String, Object> root = (Map<String, Object>) parsed

        final ExtractedToolCall shapeA = fromNameParameters(root, candidate, allowedToolNames)
        if (shapeA != null) {
            return shapeA
        }

        final ExtractedToolCall shapeB = fromToolInput(root, candidate, allowedToolNames)
        if (shapeB != null) {
            return shapeB
        }

        final ExtractedToolCall shapeC = fromSingleToolKey(root, candidate, allowedToolNames)
        if (shapeC != null) {
            return shapeC
        }

        return null
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
        if ('echo'.equals(toolName) && !(obj instanceof Map)) {
            return new LinkedHashMap<String, Object>()
        }
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
        return normalizeParams(toolName, params)
    }

    private Map<String, Object> normalizeParams(final String toolName, final Map<String, Object> params) {
        if ('echo'.equals(toolName)) {
            if (looksLikeEchoNoise(params)) {
                return new LinkedHashMap<String, Object>()
            }
            return params
        }
        return params
    }

    private boolean looksLikeEchoNoise(final Map<String, Object> params) {
        if (params.isEmpty()) {
            return false
        }
        if (params.containsKey('args')) {
            return true
        }
        int nullishCount = 0
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            final Object value = entry.getValue()
            if (value == null) {
                nullishCount++
                continue
            }
            final String s = asString(value)
            if ('<null>'.equals(s) || 'null'.equals(s)) {
                nullishCount++
            }
        }
        return nullishCount > 0 && nullishCount >= params.size()
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

    private String extractJsonCandidate(final String text) {
        final String trimmed = text.trim()

        final int fenceStart = trimmed.indexOf('```')
        if (fenceStart >= 0) {
            final int firstLineEnd = trimmed.indexOf('\n', fenceStart)
            if (firstLineEnd > fenceStart) {
                final int fenceEnd = trimmed.indexOf('```', firstLineEnd + 1)
                if (fenceEnd > firstLineEnd) {
                    final String inside = trimmed.substring(firstLineEnd + 1, fenceEnd).trim()
                    final String byBraces = sliceFirstBalancedObject(inside)
                    if (byBraces != null) {
                        return byBraces
                    }
                }
            }
        }

        return sliceFirstBalancedObject(trimmed)
    }

    private String sliceFirstBalancedObject(final String text) {
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
                    return text.substring(start, i + 1)
                }
            }
        }

        return null
    }

    private static String asString(final Object value) {
        return value == null ? null : String.valueOf(value)
    }
}
