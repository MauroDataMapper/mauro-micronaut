package org.maurodata.service.chat

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Singleton
import org.maurodata.plugin.chat.api.chat.CapabilitiesDto
import org.maurodata.plugin.chat.api.chat.McpServerDto
import org.maurodata.plugin.chat.api.chat.ModelDto
import org.maurodata.plugin.chat.api.chat.ProviderDto
import org.maurodata.plugin.chat.api.chat.SkillSummaryDto
import org.maurodata.service.chat.capabilities.CapabilitiesProvider

@CompileStatic
@Slf4j
@Singleton
class ChatCapabilitiesApiService implements ChatCapabilityService {

    private final List<CapabilitiesProvider> providers
    private final ChatSkillService chatSkillService
    private final ChatMcpService chatMcpService

    ChatCapabilitiesApiService(
        final List<CapabilitiesProvider> providers,
        final ChatSkillService chatSkillService,
        final ChatMcpService chatMcpService
    ) {
        this.providers = providers
        this.chatSkillService = chatSkillService
        this.chatMcpService = chatMcpService
    }

    @Override
    CapabilitiesDto getCapabilities() {
        final List<ModelDto> discoveredModels = new ArrayList<ModelDto>()
        final List<ProviderDto> providerDtos = new ArrayList<ProviderDto>(providers.size())

        for (int i = 0; i < providers.size(); i++) {
            final CapabilitiesProvider provider = providers.get(i)

            try {
                final ProviderDto status = provider.providerStatus()
                if (status != null) {
                    providerDtos.add(status)
                } else {
                    providerDtos.add(fallbackStatus(provider.providerId(), 'INVALID', 'No provider status'))
                }
            } catch (Throwable t) {
                log.warn('Failed providerStatus for {}: {}', provider.providerId(), t.getMessage())
                providerDtos.add(fallbackStatus(provider.providerId(), 'INVALID', t.getMessage()))
            }

            try {
                final List<ModelDto> models = provider.listModels()
                if (models != null) {
                    for (int j = 0; j < models.size(); j++) {
                        final ModelDto m = models.get(j)
                        if (m == null || isBlank(m.id)) {
                            continue
                        }
                        if (isBlank(m.provider)) {
                            m.provider = provider.providerId()
                        }
                        if (m.streaming == null) {
                            m.streaming = Boolean.TRUE
                        }
                        discoveredModels.add(m)
                    }
                }
            } catch (Throwable t) {
                log.warn('Failed listModels for {}: {}', provider.providerId(), t.getMessage())
            }
        }

        final CapabilitiesDto dto = new CapabilitiesDto()
        dto.models = dedupeModels(discoveredModels)
        dto.providers = providerDtos
        dto.skills = safeSkills()
        dto.mcpServers = safeMcpServers()
        dto.limits = new LinkedHashMap<String, Object>()
        dto.limits.put('maxInputTokens', Integer.valueOf(64000))
        dto.limits.put('maxOutputTokens', Integer.valueOf(8192))
        return dto
    }

    private List<SkillSummaryDto> safeSkills() {
        try {
            final List<SkillSummaryDto> skills = chatSkillService.listSkills()
            return skills == null ? Collections.<SkillSummaryDto>emptyList() : skills
        } catch (Throwable t) {
            log.warn('Failed to load skills: {}', t.getMessage())
            return Collections.<SkillSummaryDto>emptyList()
        }
    }

    private List<McpServerDto> safeMcpServers() {
        try {
            final List<McpServerDto> servers = chatMcpService.listServers()
            return servers == null ? Collections.<McpServerDto>emptyList() : servers
        } catch (Throwable t) {
            log.warn('Failed to load MCP servers: {}', t.getMessage())
            return Collections.<McpServerDto>emptyList()
        }
    }

    private static List<ModelDto> dedupeModels(final List<ModelDto> inModels) {
        final Map<String, ModelDto> byId = new LinkedHashMap<String, ModelDto>()
        for (int i = 0; i < inModels.size(); i++) {
            final ModelDto model = inModels.get(i)
            if (model == null || isBlank(model.id) || isBlank(model.provider)) {
                continue
            }
            final String key = model.provider + '::' + model.id
            byId.put(key, model)
        }
        return new ArrayList<ModelDto>(byId.values())
    }

    private static ProviderDto fallbackStatus(final String id, final String status, final String message) {
        final ProviderDto dto = new ProviderDto()
        dto.id = id
        dto.status = status
        dto.message = message
        return dto
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty()
    }
}
