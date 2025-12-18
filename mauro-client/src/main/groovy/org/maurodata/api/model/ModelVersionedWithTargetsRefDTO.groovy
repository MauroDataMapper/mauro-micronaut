package org.maurodata.api.model

import com.fasterxml.jackson.annotation.JsonInclude
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import org.maurodata.domain.model.AdministeredItem

@CompileStatic
@JsonInclude(JsonInclude.Include.ALWAYS)
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ModelVersionedWithTargetsRefDTO extends ModelVersionedRefDTO{

    boolean isNewBranchModelVersion=false
    boolean isNewDocumentationVersion=false
    boolean isNewFork=false

    List<VersionLinkTargetDTO> targets = []

    ModelVersionedWithTargetsRefDTO(AdministeredItem administeredItem) {
        super(administeredItem)
    }

}
