package org.maurodata.api.chat

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class UpdateSessionRequest {
    String title
}
