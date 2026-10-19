package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ItemLinkDTO {
    UUID id
    String type
    String path
    UUID sourceId
    String sourceDomainType
    UUID targetId
    String targetDomainType
}
