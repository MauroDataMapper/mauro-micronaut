package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@Introspected
@CompileStatic
class ProviderKeyStatusDto {
    String provider
    String status // SET | NOT_SET | INVALID
    Instant updatedAt
}
