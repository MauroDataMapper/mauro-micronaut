package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.model.InstantConverter
import uk.ac.ox.softeng.mauro.domain.model.Item

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient
import uk.ac.ox.softeng.mauro.domain.diff.*

import java.time.Instant

@CompileStatic
@MappedEntity(value = 'summary_metadata_report', schema = 'core', alias = 'summary_metadata_report_')
@AutoClone
class SummaryMetadataReport extends Item implements DiffableItem<SummaryMetadataReport> {
    @JsonAlias(['report_value'])
    String reportValue

    @JsonAlias(['summary_metadata_id'])
    UUID summaryMetadataId

    @JsonDeserialize(converter = InstantConverter)
    @JsonAlias(['report_date'])
    Instant reportDate

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new SummaryMetadataReportDiff(id, reportDate)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        reportDate
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<SummaryMetadataReport> diff(SummaryMetadataReport other) {
        ObjectDiff<SummaryMetadataReport> base = DiffBuilder.objectDiff(SummaryMetadataReport)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)

        base.appendString(DiffBuilder.VALUE, this.reportValue, other.reportValue)
        base.appendField(DiffBuilder.REPORT_DATE ,this.reportDate, other.reportDate)
        base

    }
}
