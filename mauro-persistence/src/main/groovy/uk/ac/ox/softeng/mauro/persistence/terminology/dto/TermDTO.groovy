package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.terminology.Term

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType

@CompileStatic
@Introspected
class TermDTO extends Term {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    List<Metadata> metadata = []
}
