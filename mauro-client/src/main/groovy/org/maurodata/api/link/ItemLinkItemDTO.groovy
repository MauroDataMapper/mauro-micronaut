package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.domain.model.Breadcrumb

@Introspected
@CompileStatic
class ItemLinkItemDTO {
    UUID id
    String domainType
    String label
    String description
    ItemLinkModelDTO owningModel
    List<Breadcrumb> breadcrumbs = []
    List<ItemLinkMetadataDTO> metadata = []
    List<ItemLinkAnnotationDTO> annotations = []
}
