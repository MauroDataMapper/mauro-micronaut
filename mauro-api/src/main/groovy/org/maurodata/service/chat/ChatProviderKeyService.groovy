package org.maurodata.service.chat

import org.maurodata.api.chat.ProviderKeyStatusDto
import org.maurodata.api.chat.UpsertProviderKeyRequest

interface ChatProviderKeyService {
    List<ProviderKeyStatusDto> listProviderKeyStatus()
    ProviderKeyStatusDto upsertProviderKey(String provider, UpsertProviderKeyRequest request)
    void removeProviderKey(String provider)
}
