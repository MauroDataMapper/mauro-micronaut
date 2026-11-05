package org.maurodata.api.link

import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable List<ItemLinkMetadataDTO> metadata
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Nullable List<ItemLinkAnnotationDTO> annotations
}
