package org.maurodata.service.chat.llm

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.util.concurrent.TimeoutException

@CompileStatic
class ProviderErrorClassifier {

    private static final JsonSlurper SLURPER = new JsonSlurper()

    static ProviderError configuration(String providerId, String message) {
        of(providerId, ProviderErrorKind.CONFIGURATION, false, null, null, null, message, Collections.<String, Object>emptyMap())
    }

    static ProviderError contextLimit(String providerId, String message, Map<String, Object> raw = [:]) {
        of(providerId, ProviderErrorKind.CONTEXT_LIMIT, false, null, null, null, message, raw)
    }

    static ProviderError toolExecution(String providerId, String message) {
        of(providerId, ProviderErrorKind.TOOL_EXECUTION, false, null, null, null, message, Collections.<String, Object>emptyMap())
    }

    static ProviderError classify(String providerId, Throwable throwable) {
        if (throwable instanceof HttpTimeoutException ||
            throwable instanceof SocketTimeoutException ||
            throwable instanceof TimeoutException) {
            return of(providerId, ProviderErrorKind.TIMEOUT, true, null, null, null, throwable.message, Collections.<String, Object>emptyMap())
        }
        if (throwable instanceof ConnectException) {
            return of(providerId, ProviderErrorKind.NETWORK, true, null, null, null, throwable.message, Collections.<String, Object>emptyMap())
        }
        classify(providerId, throwable?.message ?: throwable?.class?.simpleName ?: 'Provider error', null, Collections.<String, Object>emptyMap())
    }

    static ProviderError classify(String providerId, String message, Integer httpStatus = null, Map<String, Object> raw = [:]) {
        Map<String, Object> parsed = parseProviderError(message)
        String providerType = stringValue(parsed.get('type'))
        String providerCode = stringValue(parsed.get('code'))
        String providerMessage = stringValue(parsed.get('message')) ?: message
        Integer status = httpStatus ?: integerValue(parsed.get('status'))

        ProviderErrorKind kind = kindFor(status, providerType, providerCode, providerMessage)
        of(providerId, kind, retryable(kind), status, providerType, providerCode, providerMessage, raw ?: parsed)
    }

    static Boolean retryable(ProviderErrorKind kind) {
        switch (kind) {
            case ProviderErrorKind.RATE_LIMIT:
            case ProviderErrorKind.TIMEOUT:
            case ProviderErrorKind.NETWORK:
            case ProviderErrorKind.SERVER:
                return Boolean.TRUE
            case ProviderErrorKind.CONFIGURATION:
            case ProviderErrorKind.AUTHENTICATION:
            case ProviderErrorKind.AUTHORIZATION:
            case ProviderErrorKind.QUOTA:
            case ProviderErrorKind.MODEL_UNAVAILABLE:
            case ProviderErrorKind.CONTEXT_LIMIT:
            case ProviderErrorKind.TOOL_EXECUTION:
            case ProviderErrorKind.UNKNOWN:
            default:
                return Boolean.FALSE
        }
    }

    private static ProviderErrorKind kindFor(Integer httpStatus, String providerType, String providerCode, String message) {
        String type = normalize(providerType)
        String code = normalize(providerCode)
        String text = normalize(message)

        if (httpStatus != null && httpStatus >= 500) {
            return ProviderErrorKind.SERVER
        }
        if (httpStatus == 401 || code.contains('invalid_api_key') || text.contains('invalid api key') || text.contains('missing api key')) {
            return ProviderErrorKind.AUTHENTICATION
        }
        if (httpStatus == 403 || text.contains('forbidden') || text.contains('permission denied') || text.contains('access denied')) {
            return ProviderErrorKind.AUTHORIZATION
        }
        if (code.contains('insufficient_quota') || type.contains('insufficient_quota') || text.contains('quota') || text.contains('billing')) {
            return ProviderErrorKind.QUOTA
        }
        if (httpStatus == 429 || type.contains('rate_limit') || code.contains('rate_limit')) {
            return ProviderErrorKind.RATE_LIMIT
        }
        if (httpStatus == 404 || code.contains('model_not_found') || text.contains('model not found')) {
            return ProviderErrorKind.MODEL_UNAVAILABLE
        }
        if (text.contains('context/output limit') || text.contains('context limit') || text.contains('output limit') || text.contains('prompt_eval_count')) {
            return ProviderErrorKind.CONTEXT_LIMIT
        }
        ProviderErrorKind.UNKNOWN
    }

    private static ProviderError of(
        String providerId,
        ProviderErrorKind kind,
        Boolean retryable,
        Integer httpStatus,
        String providerType,
        String providerCode,
        String message,
        Map<String, Object> raw
    ) {
        new ProviderError(
            providerId: providerId,
            kind: kind ?: ProviderErrorKind.UNKNOWN,
            retryable: retryable,
            httpStatus: httpStatus,
            providerErrorType: providerType,
            providerErrorCode: providerCode,
            message: message,
            raw: raw ?: Collections.<String, Object>emptyMap()
        )
    }

    private static Map<String, Object> parseProviderError(String message) {
        if (message == null || !message.contains('{')) {
            return Collections.<String, Object>emptyMap()
        }
        String json = message.substring(message.indexOf('{')).trim()
        try {
            Object parsed = SLURPER.parseText(json)
            if (parsed instanceof Map) {
                Object error = ((Map<?, ?>) parsed).get('error')
                if (error instanceof Map) {
                    return new LinkedHashMap<String, Object>((Map<String, Object>) error)
                }
                return new LinkedHashMap<String, Object>((Map<String, Object>) parsed)
            }
        } catch (Exception ignored) {
            return Collections.<String, Object>emptyMap()
        }
        Collections.<String, Object>emptyMap()
    }

    private static String normalize(String value) {
        value == null ? '' : value.toLowerCase(Locale.ROOT)
    }

    private static String stringValue(Object value) {
        value == null ? null : String.valueOf(value)
    }

    private static Integer integerValue(Object value) {
        if (value == null) {
            return null
        }
        value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : Integer.valueOf(String.valueOf(value))
    }
}
