package uk.ac.ox.softeng.mauro.persistence.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@Introspected
@CompileStatic
class SearchDTO  {


    UUID id
    String domainType
    String label
    String description
    Instant dateCreated
    Instant lastUpdated


    Float tsRank



}
