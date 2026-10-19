package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class ItemLinkItemDTO {
    UUID id
    String domainType
    String label
    String description
    ItemLinkModelDTO owningModel
    List<ItemLinkMetadataDTO> metadata = []
    List<ItemLinkAnnotationDTO> annotations = []
}
