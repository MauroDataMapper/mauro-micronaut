package org.maurodata.api.chat

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.http.annotation.Get

@MauroApi
interface ChatCapabilitiesApi {

    @Get(Paths.CHAT_CAPABILITIES)
    CapabilitiesDto getCapabilities()
}
