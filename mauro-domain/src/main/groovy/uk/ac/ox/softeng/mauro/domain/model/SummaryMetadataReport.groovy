package uk.ac.ox.softeng.mauro.domain.model

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.MappedEntity

@CompileStatic
@MappedEntity(value = 'summary_metadata_report', schema = 'core', alias = 'summary_metadata_report_')
@AutoClone
class SummaryMetadataReport extends Item{
    String reportValue

    UUID summaryMetadataId
}
