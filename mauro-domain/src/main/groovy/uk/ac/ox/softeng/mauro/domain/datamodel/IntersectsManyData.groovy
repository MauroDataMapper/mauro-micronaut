package uk.ac.ox.softeng.mauro.domain.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class IntersectsManyData {

    List<UUID> targetDataModelIds
    List<UUID> dataElementIds

}
