package org.maurodata.service.chat

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.maurodata.api.chat.ProviderKeyStatusDto
import org.maurodata.api.chat.ProviderDto
import org.maurodata.api.chat.UpsertProviderKeyRequest
import org.maurodata.service.chat.capabilities.CapabilitiesProvider

import java.time.Instant

@CompileStatic
@Singleton
class ChatProviderKeysApiService implements ChatProviderKeyService {

    private final List<CapabilitiesProvider> providers

    ChatProviderKeysApiService(final List<CapabilitiesProvider> providers) {
        this.providers = providers
    }

    @Override
    List<ProviderKeyStatusDto> listProviderKeyStatus() {
        final List<ProviderKeyStatusDto> out = new ArrayList<ProviderKeyStatusDto>(providers.size())
        final Instant now = Instant.now()

        for (int i = 0; i < providers.size(); i++) {
            final CapabilitiesProvider provider = providers.get(i)
            final ProviderDto providerStatus = provider.providerStatus()

            final ProviderKeyStatusDto dto = new ProviderKeyStatusDto()
            dto.provider = provider.providerId()
            dto.status = normalizeStatus(providerStatus.status)
            dto.updatedAt = now
            out.add(dto)
        }
        return out
    }

    @Override
    ProviderKeyStatusDto upsertProviderKey(String provider, UpsertProviderKeyRequest request) {
        final CapabilitiesProvider matched = findProvider(provider)
        if (matched == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Provider not found: ${provider}")
        }
        final ProviderDto providerStatus = matched.providerStatus()
        final ProviderKeyStatusDto dto = new ProviderKeyStatusDto()
        dto.provider = matched.providerId()
        dto.status = normalizeStatus(providerStatus.status)
        dto.updatedAt = Instant.now()
        return dto
    }

    private CapabilitiesProvider findProvider(final String providerId) {
        for (int i = 0; i < providers.size(); i++) {
            final CapabilitiesProvider provider = providers.get(i)
            if (provider.providerId().equals(providerId)) {
                return provider
            }
        }
        return null
    }

    private static String normalizeStatus(final String status) {
        if (status == null || status.trim().isEmpty()) {
            return 'INVALID'
        }
        if ('SET'.equals(status) || 'NOT_SET'.equals(status) || 'INVALID'.equals(status)) {
            return status
        }
        if ('CONNECTED'.equals(status)) {
            return 'SET'
        }
        if ('MISSING_KEY'.equals(status)) {
            return 'NOT_SET'
        }
        return 'INVALID'
    }
}
