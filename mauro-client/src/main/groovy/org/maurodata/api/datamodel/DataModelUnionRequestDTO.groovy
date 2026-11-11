package org.maurodata.api.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.domain.model.ModelPathMatch

/** Request for building a fresh structural union from visible DataModels in a container. */
@CompileStatic
@Introspected
class DataModelUnionRequestDTO {
    String rootDomainType
    UUID rootId
    UUID folderId
    String label
    /** Optional path match relative to the supplied root. */
    ModelPathMatch modelPathMatch
}
