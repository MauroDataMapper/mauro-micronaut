package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.ManyToOne

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
class SummaryMetadataReport {
    String reportValue

    @ManyToOne
    @JsonAlias(['summary_metadata_type'])
    @JsonDeserialize(converter = SummaryMetadataType.SummaryMetadataTypeConverter)
    SummaryMetadata summaryMetadata

}
