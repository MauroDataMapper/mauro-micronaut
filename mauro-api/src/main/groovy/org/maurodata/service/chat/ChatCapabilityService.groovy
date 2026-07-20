package org.maurodata.service.chat

import org.maurodata.plugin.chat.api.chat.CapabilitiesDto
import groovy.transform.CompileStatic

@CompileStatic
interface ChatCapabilityService {
    CapabilitiesDto getCapabilities()
}
