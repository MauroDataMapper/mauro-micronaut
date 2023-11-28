package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.Version
import io.micronaut.data.model.DataType
import jakarta.persistence.Transient

import java.time.Instant

@CompileStatic
//@Introspected(classes = [Facet, AdministeredItem])
@AutoClone(excludes = ['id', 'version'])
abstract class Facet {

    @Id
    @GeneratedValue
    UUID id

    @Version
    Integer version

    @DateCreated
    Instant dateCreated

    @DateUpdated
    Instant lastUpdated

//    @Nullable
    String multiFacetAwareItemDomainType

    UUID multiFacetAwareItemId

//    @TypeDef(type = DataType.UUID)
//    @Relation(value = Relation.Kind.ONE_TO_ONE)
//    @MappedProperty('multi_facet_aware_item_id')
    @Transient
    AdministeredItem multiFacetAwareItem

    @Nullable
    String createdBy
}
