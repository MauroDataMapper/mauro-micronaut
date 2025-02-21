package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import uk.ac.ox.softeng.mauro.domain.diff.*

@CompileStatic
@MappedEntity(value = 'summary_metadata', schema = 'core', alias = 'summary_metadata_')
@AutoClone()
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class SummaryMetadata extends Facet implements DiffableItem<SummaryMetadata> {

    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String label

    String description

    @JsonAlias(['summary_metadata_type'])
    @JsonDeserialize(converter = SummaryMetadataType.SummaryMetadataTypeConverter)
    SummaryMetadataType summaryMetadataType

    @JsonAlias(['summary_metadata_reports'])
    @Relation(Relation.Kind.ONE_TO_MANY)
    List<SummaryMetadataReport> summaryMetadataReports = []

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new SummaryMetadataDiff(id, summaryMetadataType, label)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        label
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<SummaryMetadata> diff(SummaryMetadata other) {
        ObjectDiff<SummaryMetadata> base = DiffBuilder.objectDiff(SummaryMetadata)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description)
        base.appendString(DiffBuilder.SUMMARY_METADATA_TYPE, this.summaryMetadataType.name(), other.summaryMetadataType.name())
        if (!DiffBuilder.isNull(this.summaryMetadataReports) ||!DiffBuilder.isNull(other.summaryMetadataReports)) {
            base.appendCollection(DiffBuilder.SUMMARY_METADATA_REPORT, this.summaryMetadataReports as Collection<DiffableItem>, other.summaryMetadataReports as Collection<DiffableItem>)
        }
        base
    }

    /****
     * Methods for building a tree-like DSL
     */

    static SummaryMetadata build(
        Map args,
        @DelegatesTo(value = SummaryMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new SummaryMetadata(args).tap(closure)
    }

    static SummaryMetadata build(
        @DelegatesTo(value = SummaryMetadata, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    /**
     * DSL helper method for setting the label.  Returns the label passed in.
     *
     * @see #label
     */
    String label(String label) {
        this.label = label
        this.label
    }

    /**
     * DSL helper method for setting the description.  Returns the description passed in.
     *
     * @see #description
     */
    String description(String description) {
        this.description = description
        this.description
    }

    /**
     * DSL helper method for setting the summaryMetadataType.  Returns the type passed in.
     *
     * @see #summaryMetadataType
     */
    SummaryMetadataType summaryMetadataType(SummaryMetadataType summaryMetadataType) {
        this.summaryMetadataType = summaryMetadataType
        this.summaryMetadataType
    }

    SummaryMetadataReport summaryMetadataReport(SummaryMetadataReport summaryMetadataReport) {
        this.summaryMetadataReports.add(summaryMetadataReport)
        summaryMetadataReport.summaryMetadataId = this.id
        summaryMetadataReport
    }

    SummaryMetadataReport summaryMetadataReport(Map args, @DelegatesTo(value = SummaryMetadataReport, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        SummaryMetadataReport smr = SummaryMetadataReport.build(args + [summaryMetadataId: this.id], closure)
        summaryMetadataReport(smr)
    }

    SummaryMetadataReport summaryMetadataReport(@DelegatesTo(value = SummaryMetadataReport, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        summaryMetadataReport [:], closure
    }

}
