package org.maurodata.api.model

import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.MapConstructor
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model

@JsonInclude(JsonInclude.Include.ALWAYS)
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ModelVersionedRefDTO extends ModelRefDTO{

    String branch // version model tree calls this branch
    String branchName
    String modelVersion
    String modelVersionTag
    String documentationVersion
    String displayName

    ModelVersionedRefDTO(AdministeredItem administeredItem) {
        super(administeredItem)
        if(administeredItem instanceof Model) {
            branch = administeredItem.branchName
            branchName = administeredItem.branchName
            modelVersion = administeredItem.modelVersion?.toString()
            modelVersionTag = administeredItem.modelVersionTag
            documentationVersion = administeredItem.documentationVersion
            displayName = administeredItem.pathModelIdentifier
        }
    }
}
