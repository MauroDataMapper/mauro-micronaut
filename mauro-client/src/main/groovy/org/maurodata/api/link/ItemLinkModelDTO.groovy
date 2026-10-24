package org.maurodata.api.link

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import org.maurodata.domain.model.version.ModelVersion

@Introspected
@CompileStatic
class ItemLinkModelDTO {
    UUID id
    String domainType
    String label
    Boolean finalised
    String branchName
    ModelVersion modelVersion
    String modelVersionTag
}
