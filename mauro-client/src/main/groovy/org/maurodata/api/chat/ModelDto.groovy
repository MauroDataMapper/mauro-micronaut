package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ModelDto {
    String id
    String provider
    Boolean streaming
    Boolean tools
    Integer contextWindow
}
