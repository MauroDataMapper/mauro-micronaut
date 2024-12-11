package uk.ac.ox.softeng.mauro.persistence.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Transient

import java.time.Instant

@Introspected
@CompileStatic
class SearchResultsDTO {


    UUID id
    String domainType
    String label
    String description
    Instant dateCreated
    Instant lastUpdated


    Float tsRank

    @Deprecated
    @Transient
    UUID model


}
