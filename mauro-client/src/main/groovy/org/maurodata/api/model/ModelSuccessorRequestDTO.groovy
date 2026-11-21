package org.maurodata.api.model

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.domain.model.version.FinaliseData

@CompileStatic
@Introspected
class ModelSuccessorRequestDTO {
    UUID successorModelId
    /** Optional override; otherwise membership changes select MAJOR or MINOR. */
    FinaliseData finaliseData
}
