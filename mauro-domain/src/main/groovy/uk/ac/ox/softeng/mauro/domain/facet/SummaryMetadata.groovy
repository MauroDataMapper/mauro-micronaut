package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport

@CompileStatic
@MappedEntity(value = 'summary_metadata', schema = 'core', alias = 'summary_metadata_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class SummaryMetadata extends Facet{

    @JsonAlias(['summary_metadata_type'])
    @JsonDeserialize(converter = SummaryMetadataType.SummaryMetadataTypeConverter)
    SummaryMetadataType summaryMetadataType

    @JsonAlias(['summary_metadata_reports'])
    @Relation(Relation.Kind.ONE_TO_MANY)
    List<SummaryMetadataReport> summaryMetadataReports

}
