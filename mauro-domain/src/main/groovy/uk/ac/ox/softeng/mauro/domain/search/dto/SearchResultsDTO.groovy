package uk.ac.ox.softeng.mauro.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.persistence.Transient

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
    @Transient
    UUID modelId

    Float tsRank



}
