package uk.ac.ox.softeng.mauro.persistence.search.dto

import uk.ac.ox.softeng.mauro.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable

import java.sql.Date

@Introspected
@CompileStatic
class SearchRequestDTO extends PaginationParams {

    @Nullable
    String searchTerm

    @Nullable
    List<String> domainTypes = []

    @Nullable
    UUID withinModelId

    // boolean labelOnly // TODO: Implement support for this

    @Nullable
    Boolean prefixSearch = false

    @Nullable
    Date lastUpdatedAfter
    @Nullable
    Date lastUpdatedBefore
    @Nullable
    Date createdAfter
    @Nullable
    Date createdBefore

    @Nullable
    List<UUID> classifiers // TODO: Implement support for this

    // List<MetadataFields> profileFields // TODO: Implement support for this

}
