package org.maurodata.domain.facet

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.maurodata.domain.diff.*

@CompileStatic
@MappedEntity(value = 'summary_metadata', schema = 'core', alias = 'summary_metadata_')
@AutoClone()
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class SummaryMetadata extends Facet implements DiffableItem<SummaryMetadata>, ItemReferencer {

    @NotBlank
    String label

    String description

    @JsonAlias(['summary_metadata_type'])
    @JsonDeserialize(converter = SummaryMetadataType.SummaryMetadataTypeConverter)
    SummaryMetadataType summaryMetadataType

    @JsonAlias(['summary_metadata_reports'])
    @Relation(Relation.Kind.ONE_TO_MANY)
    Collection<SummaryMetadataReport> summaryMetadataReports = []

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new SummaryMetadataDiff(id, summaryMetadataType, label, summaryMetadataReports, getDiffIdentifier())
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (multiFacetAwareItem != null) {return "${multiFacetAwareItem.getDiffIdentifier()}|${this.pathNodeString}"}
        return "${this.pathNodeString}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<SummaryMetadata> diff(SummaryMetadata other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<SummaryMetadata> base = DiffBuilder.objectDiff(SummaryMetadata)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.SUMMARY_METADATA_TYPE, this.summaryMetadataType.name(), other.summaryMetadataType.name(), this, other)
        if (!DiffBuilder.isNull(this.summaryMetadataReports) || !DiffBuilder.isNull(other.summaryMetadataReports)) {
            base.appendCollection(DiffBuilder.SUMMARY_METADATA_REPORT, this.summaryMetadataReports as Collection<DiffableItem>,
                                  other.summaryMetadataReports as Collection<DiffableItem>, lhsPathRoot, rhsPathRoot)
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

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'sm'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        label
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(summaryMetadataReports, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)
        summaryMetadataReports = ItemReferencerUtils.replaceItemsByIdentity(summaryMetadataReports, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        SummaryMetadata intoSummaryMetadata = (SummaryMetadata) into
        intoSummaryMetadata.label = ItemUtils.copyItem(this.label, intoSummaryMetadata.label)
        intoSummaryMetadata.description = ItemUtils.copyItem(this.description, intoSummaryMetadata.description)
        intoSummaryMetadata.summaryMetadataType = ItemUtils.copyItem(this.summaryMetadataType, intoSummaryMetadata.summaryMetadataType)
        intoSummaryMetadata.summaryMetadataReports = ItemUtils.copyItems(this.summaryMetadataReports, intoSummaryMetadata.summaryMetadataReports)
    }

    @Override
    Item shallowCopy() {
        SummaryMetadata summaryMetadataShallowCopy = new SummaryMetadata()
        this.copyInto(summaryMetadataShallowCopy)
        return summaryMetadataShallowCopy
    }
}
