package uk.ac.ox.softeng.mauro.domain.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.MappedEntity

import java.time.Instant

@CompileStatic
@MappedEntity(value = 'summary_metadata_report', schema = 'core', alias = 'summary_metadata_report_')
@AutoClone
class SummaryMetadataReport extends Item{
    @JsonAlias(['report_value'])
    String reportValue

    @JsonAlias(['summary_metadata_id'])
    UUID summaryMetadataId

    @JsonDeserialize(converter = InstantConverter)
    @JsonAlias(['report_date'])
    Instant reportDate

}
