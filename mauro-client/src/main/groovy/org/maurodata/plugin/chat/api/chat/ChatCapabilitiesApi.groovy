package org.maurodata.plugin.chat.api.chat

import org.maurodata.api.MauroApi
import org.maurodata.plugin.chat.api.Paths

import io.micronaut.http.annotation.Get

@MauroApi
interface ChatCapabilitiesApi {

    @Get(Paths.CHAT_CAPABILITIES)
    CapabilitiesDto getCapabilities()
}
