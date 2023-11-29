package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType
import jakarta.persistence.Transient

@CompileStatic
@Introspected
class TerminologyDTO extends Terminology {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    List<Metadata> metadata = []
}
