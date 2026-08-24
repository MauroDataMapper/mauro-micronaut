package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
class ProviderError {
    ProviderErrorKind kind = ProviderErrorKind.UNKNOWN
    Boolean retryable
    String providerId
    Integer httpStatus
    String providerErrorType
    String providerErrorCode
    String message
    Map<String, Object> raw = [:]

    Map<String, Object> toMetadata() {
        Map<String, Object> out = new LinkedHashMap<String, Object>()
        out.put('kind', kind?.name())
        out.put('retryable', retryable)
        out.put('providerId', providerId)
        out.put('httpStatus', httpStatus)
        out.put('providerErrorType', providerErrorType)
        out.put('providerErrorCode', providerErrorCode)
        out.put('message', message)
        out.put('raw', raw ?: [:])
        out
    }
}
