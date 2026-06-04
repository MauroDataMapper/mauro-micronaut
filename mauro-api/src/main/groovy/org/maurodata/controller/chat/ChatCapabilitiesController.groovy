package org.maurodata.controller.chat

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.maurodata.api.Paths
import org.maurodata.api.chat.CapabilitiesDto
import org.maurodata.api.chat.ChatCapabilitiesApi
import org.maurodata.audit.Audit
import org.maurodata.service.chat.ChatCapabilityService

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ChatCapabilitiesController implements ChatCapabilitiesApi {

    private final ChatCapabilityService chatCapabilityService

    ChatCapabilitiesController(ChatCapabilityService chatCapabilityService) {
        this.chatCapabilityService = chatCapabilityService
    }

    @Override
    @Audit
    @Get(Paths.CHAT_CAPABILITIES)
    CapabilitiesDto getCapabilities() {
        chatCapabilityService.getCapabilities()
    }
}
