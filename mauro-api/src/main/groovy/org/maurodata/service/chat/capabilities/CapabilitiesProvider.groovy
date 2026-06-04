package org.maurodata.service.chat.capabilities

import groovy.transform.CompileStatic
import org.maurodata.api.chat.ModelDto
import org.maurodata.api.chat.ProviderDto

@CompileStatic
interface CapabilitiesProvider {
    String providerId()
    List<ModelDto> listModels()
    ProviderDto providerStatus()
}
