package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class SummaryMetadata extends Facet{


    SummaryMetadataType summaryMetadataType

    @JsonIgnore
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'summaryMetadata')
    Set<SummaryMetadataReport> summaryMetadataReports = []

}
