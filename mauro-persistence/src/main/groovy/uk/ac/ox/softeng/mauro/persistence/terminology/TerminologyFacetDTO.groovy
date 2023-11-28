package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType

class TerminologyFacetDTO extends Terminology {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    List<Metadata> metadata = []
}
