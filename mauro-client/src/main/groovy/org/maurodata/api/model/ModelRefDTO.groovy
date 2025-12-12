package org.maurodata.api.model

import groovy.transform.MapConstructor
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Breadcrumb

@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ModelRefDTO {
    UUID id
    String domainType
    String label
    UUID model
    List<Breadcrumb> breadcrumbs

    ModelRefDTO(AdministeredItem administeredItem) {
        id = administeredItem.id
        label = administeredItem.label
        domainType = administeredItem.domainType
        model = administeredItem.owner?.id
        breadcrumbs = administeredItem.breadcrumbs
    }

}
