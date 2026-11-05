package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ItemLinkMetadataDTO {
    UUID id
    String namespace
    String key
    String value
}
