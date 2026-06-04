package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class CapabilitiesDto {
    List<ModelDto> models = []
    List<ProviderDto> providers = []
    List<McpServerDto> mcpServers = []
    List<SkillSummaryDto> skills = []
    Map<String, Object> limits = [:]
}
