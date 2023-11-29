package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType
import jakarta.persistence.Transient

@CompileStatic
@Introspected
trait MetadataAware {

    @Transient
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = terminology.id)')
    List<Metadata> metadata = []
}