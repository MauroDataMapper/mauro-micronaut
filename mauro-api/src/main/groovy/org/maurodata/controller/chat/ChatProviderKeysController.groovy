package org.maurodata.controller.chat

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import io.micronaut.http.HttpStatus
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.validation.Valid
import org.maurodata.api.Paths
import org.maurodata.api.chat.ChatProviderKeysApi
import org.maurodata.api.chat.ProviderKeyStatusDto
import org.maurodata.api.chat.UpsertProviderKeyRequest
import org.maurodata.audit.Audit
import org.maurodata.service.chat.ChatProviderKeyService

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ChatProviderKeysController implements ChatProviderKeysApi {

    private final ChatProviderKeyService chatProviderKeyService

    ChatProviderKeysController(ChatProviderKeyService chatProviderKeyService) {
        this.chatProviderKeyService = chatProviderKeyService
    }

    @Override
    @Audit
    @Get(Paths.CHAT_PROVIDER_KEYS)
    List<ProviderKeyStatusDto> listProviderKeyStatus() {
        chatProviderKeyService.listProviderKeyStatus()
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.CHAT_PROVIDER_KEYS_PROVIDER)
    ProviderKeyStatusDto upsertProviderKey(@PathVariable String provider, @Body @Valid UpsertProviderKeyRequest request) {
        chatProviderKeyService.upsertProviderKey(provider, request)
    }

    @Override
    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Status(HttpStatus.NO_CONTENT)
    @Delete(Paths.CHAT_PROVIDER_KEYS_PROVIDER)
    void removeProviderKey(@PathVariable String provider) {
        chatProviderKeyService.removeProviderKey(provider)
    }
}
