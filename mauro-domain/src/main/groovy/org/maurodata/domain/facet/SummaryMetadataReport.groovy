package org.maurodata.domain.facet

import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.diff.SummaryMetadataReportDiff
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Pathable

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
import java.time.format.DateTimeFormatter

@CompileStatic
@MappedEntity(value = 'summary_metadata_report', schema = 'core', alias = 'summary_metadata_report_')
@AutoClone
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SummaryMetadataReport extends Item implements DiffableItem<SummaryMetadataReport>, Pathable {
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
        new SummaryMetadataReportDiff(id, reportDate, reportValue, getDiffIdentifier())
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        return "${pathPrefix}:${reportValue}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<SummaryMetadataReport> diff(SummaryMetadataReport other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<SummaryMetadataReport> base = DiffBuilder.objectDiff(SummaryMetadataReport)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)

        base.appendString(DiffBuilder.REPORT_VALUE, this.reportValue, other.reportValue, this, other)
        base.appendField(DiffBuilder.REPORT_DATE, this.reportDate, other.reportDate, this, other)
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

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'smr'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        if(reportDate != null) {return reportDate.toString()}
        return "-"
    }

    @Transient
    @JsonIgnore
    @Override
    @Nullable
    String getPathModelIdentifier() {
        return null
    }
}
