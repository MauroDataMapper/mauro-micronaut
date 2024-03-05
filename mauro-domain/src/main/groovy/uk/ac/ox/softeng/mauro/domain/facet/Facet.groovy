package uk.ac.ox.softeng.mauro.domain.facet

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.terminology.Term

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
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
@AutoClone
abstract class Facet extends Item {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias(['multi_facet_aware_item_domain_type'])
    String multiFacetAwareItemDomainType

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias(['multi_facet_aware_item_id'])
    UUID multiFacetAwareItemId

    @Transient
    AdministeredItem multiFacetAwareItem

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String domainType = this.class.simpleName
}
