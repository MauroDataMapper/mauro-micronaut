package org.maurodata.plugin.chat.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class SkillSummaryDto {
    String id
    String name
    String description
    String scope // GLOBAL | WORKSPACE
    String version
}
