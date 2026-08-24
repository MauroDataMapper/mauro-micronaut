package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
enum ProviderErrorKind {
    CONFIGURATION,
    AUTHENTICATION,
    AUTHORIZATION,
    QUOTA,
    RATE_LIMIT,
    MODEL_UNAVAILABLE,
    CONTEXT_LIMIT,
    TIMEOUT,
    NETWORK,
    SERVER,
    TOOL_EXECUTION,
    UNKNOWN
}
