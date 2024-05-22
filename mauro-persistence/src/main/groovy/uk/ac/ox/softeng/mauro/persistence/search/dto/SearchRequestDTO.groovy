package uk.ac.ox.softeng.mauro.persistence.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

import java.time.Instant

@Introspected
@CompileStatic
class SearchRequestDTO  {

    List<String> domainTypes

    boolean labelOnly // TODO: Implement support for this

    String searchTerm

    Date lastUpdatedAfter
    Date lastUpdatedBefore
    Date createdAfter
    Date createdBefore

    List<UUID> classifiers // TODO: Implement support for this

    // List<MetadataFields> profileFields // TODO: Implement support for this

}
