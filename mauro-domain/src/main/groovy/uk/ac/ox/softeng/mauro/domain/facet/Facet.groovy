package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item

@CompileStatic
@AutoClone
abstract class Facet<I extends Facet> extends Item {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias(['multi_facet_aware_item_domain_type'])
    String multiFacetAwareItemDomainType

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias(['multi_facet_aware_item_id'])
    UUID multiFacetAwareItemId

    @Transient
    @JsonIgnore
    AdministeredItem multiFacetAwareItem

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String domainType = this.class.simpleName
}
