package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
class SummaryMetadataReport {
    String reportValue

    @ManyToOne
    SummaryMetadata summaryMetadata

}
