package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.terminology.Term
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
@MappedEntity(value = 'term', schema = 'terminology', alias = 'term_')
class TermDTO extends Term implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = term_.id)')
    // could use @JsonRawValue if we need to speed up binding this from the DB
    List<Metadata> metadata = []
}
