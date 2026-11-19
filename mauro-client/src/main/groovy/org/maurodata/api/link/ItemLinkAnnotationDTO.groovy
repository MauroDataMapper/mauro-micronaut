package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ItemLinkAnnotationDTO {
    UUID id
    String label
    String description
    List<ItemLinkAnnotationDTO> childAnnotations = []
}
