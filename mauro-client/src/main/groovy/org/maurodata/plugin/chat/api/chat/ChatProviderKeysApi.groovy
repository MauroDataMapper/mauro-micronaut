package org.maurodata.plugin.chat.api.chat

import org.maurodata.api.MauroApi
import org.maurodata.plugin.chat.api.Paths

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.Status
import io.micronaut.http.HttpStatus
import jakarta.validation.Valid

@MauroApi
interface ChatProviderKeysApi {

    @Get(Paths.CHAT_PROVIDER_KEYS)
    List<ProviderKeyStatusDto> listProviderKeyStatus()

    @Put(Paths.CHAT_PROVIDER_KEYS_PROVIDER)
    ProviderKeyStatusDto upsertProviderKey(@PathVariable String provider, @Body @Valid UpsertProviderKeyRequest request)

    @Status(HttpStatus.NO_CONTENT)
    @Delete(Paths.CHAT_PROVIDER_KEYS_PROVIDER)
    void removeProviderKey(@PathVariable String provider)
}
