package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffBuilder
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.MetadataDiff
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.diff.SummaryMetadataDiff
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport

@CompileStatic
@MappedEntity(value = 'summary_metadata', schema = 'core', alias = 'summary_metadata_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_oaware_item_id'])])
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
    List<SummaryMetadataReport> summaryMetadataReports

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

}
