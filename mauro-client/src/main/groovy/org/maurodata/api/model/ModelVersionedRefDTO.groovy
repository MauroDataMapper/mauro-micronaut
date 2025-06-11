package org.maurodata.api.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.ALWAYS)
class ModelVersionedRefDTO extends ModelRefDTO{

    String branch // version model tree calls this branch
    String branchName
    String modelVersion
    String modelVersionTag
    String documentationVersion
    String displayName
}
