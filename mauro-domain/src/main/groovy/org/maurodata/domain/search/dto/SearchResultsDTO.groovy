package org.maurodata.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import jakarta.persistence.Transient
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.model.Breadcrumb

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

    @Transient
    List<Classifier> classifiers = []

    @Transient
    List<Breadcrumb> breadcrumbs

    Float tsRank



}
