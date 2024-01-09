package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.dto.AdministeredItemDTO

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType

@CompileStatic
@Introspected
@MappedEntity(value = 'terminology', schema = 'terminology', alias = 'terminology_')
class TerminologyDTO extends Terminology implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = terminology_.id)')
    List<Metadata> metadata = []
}
