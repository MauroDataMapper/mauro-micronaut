package org.maurodata.persistence.classifier.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@Introspected
@CompileStatic
class ClassifierJoinDTO {
    UUID classifierId
    UUID catalogueItemId
    String catalogueItemDomainType
    Instant dateCreated
    Instant lastUpdated
}
