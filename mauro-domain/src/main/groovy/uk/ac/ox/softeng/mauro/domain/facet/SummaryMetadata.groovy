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
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport

@CompileStatic
@MappedEntity(value = 'summary_metadata', schema = 'core', alias = 'summary_metadata_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_oaware_item_id'])])
class SummaryMetadata extends Facet implements DiffableItem<SummaryMetadata> {

    @NotBlank
    @Pattern(regexp = /[^\$@|]*/, message = 'Cannot contain $, | or @')
    String label

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
        return null
    }


    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        //toDo
        return null
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<SummaryMetadata> diff(SummaryMetadata other) {
        //todo
        return null
    }
}
