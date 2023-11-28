package uk.ac.ox.softeng.mauro.domain.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import jakarta.persistence.Transient

@CompileStatic
@Introspected
trait MetadataAware {

    @Transient
    List<Metadata> metadata = []
}