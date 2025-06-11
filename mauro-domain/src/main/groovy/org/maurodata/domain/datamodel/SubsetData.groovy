package org.maurodata.domain.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class SubsetData {

    List<UUID> additions
    List<UUID> deletions

}
