package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.diff.SummaryMetadataReportDiff
import uk.ac.ox.softeng.mauro.domain.model.Item

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient

import java.time.Instant

@CompileStatic
@MappedEntity(value = 'summary_metadata_report', schema = 'core', alias = 'summary_metadata_report_')
@AutoClone
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SummaryMetadataReport extends Item implements DiffableItem<SummaryMetadataReport> {
    @JsonAlias(['report_value'])
    @NonNull
    String reportValue

    @JsonAlias(['summary_metadata_id'])
    @NonNull
    UUID summaryMetadataId

    @JsonAlias(['report_date'])
    @Nullable
    Instant reportDate

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new SummaryMetadataReportDiff(id, reportDate, reportValue)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        reportValue
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<SummaryMetadataReport> diff(SummaryMetadataReport other) {
        ObjectDiff<SummaryMetadataReport> base = DiffBuilder.objectDiff(SummaryMetadataReport)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)

        base.appendString(DiffBuilder.REPORT_VALUE, this.reportValue, other.reportValue)
        base.appendField(DiffBuilder.REPORT_DATE ,this.reportDate, other.reportDate)
        base
    }

    /****
     * Methods for building a tree-like DSL
     */

    static SummaryMetadataReport build(
        Map args,
        @DelegatesTo(value = SummaryMetadataReport, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new SummaryMetadataReport(args).tap(closure)
    }

    static SummaryMetadataReport build(
        @DelegatesTo(value = SummaryMetadataReport, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    /**
     * DSL helper method for setting the reportValue.  Returns the reportValue passed in.
     *
     * @see #reportValue
     */
    String reportValue(String reportValue) {
        this.reportValue = reportValue
        this.reportValue
    }


    /**
     * DSL helper method for setting the date the date of this report.  Returns the date/time passed in.
     *
     * @see #reportDate
     */
    Instant reportDate(String reportDate) {
        this.reportDate = Instant.parse(reportDate)
        this.reportDate
    }

}
