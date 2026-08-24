package org.maurodata.service.chat.llm

import groovy.transform.CompileStatic

@CompileStatic
class ProviderOperationException extends RuntimeException {
    final ProviderError providerError

    ProviderOperationException(String message, ProviderError providerError) {
        super(message)
        this.providerError = providerError
    }
}
