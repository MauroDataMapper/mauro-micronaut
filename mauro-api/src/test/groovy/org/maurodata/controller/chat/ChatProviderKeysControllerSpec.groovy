package org.maurodata.controller.chat

import org.maurodata.plugin.chat.api.chat.ProviderKeyStatusDto
import org.maurodata.plugin.chat.api.chat.UpsertProviderKeyRequest
import org.maurodata.service.chat.ChatProviderKeyService
import spock.lang.Specification

class ChatProviderKeysControllerSpec extends Specification {

    TestProviderKeyService service = new TestProviderKeyService()
    ChatProviderKeysController controller = new ChatProviderKeysController(service)

    void 'remove provider key delegates to service'() {
        when:
        controller.removeProviderKey('openai')

        then:
        service.removedProvider == 'openai'
    }

    static class TestProviderKeyService implements ChatProviderKeyService {
        String removedProvider

        @Override
        List<ProviderKeyStatusDto> listProviderKeyStatus() {
            []
        }

        @Override
        ProviderKeyStatusDto upsertProviderKey(String provider, UpsertProviderKeyRequest request) {
            new ProviderKeyStatusDto(provider: provider, status: 'SET')
        }

        @Override
        void removeProviderKey(String provider) {
            removedProvider = provider
        }
    }
}
