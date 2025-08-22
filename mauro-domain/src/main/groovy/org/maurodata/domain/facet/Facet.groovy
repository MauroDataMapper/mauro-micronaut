package org.maurodata.domain.facet

import org.maurodata.domain.model.Path
import org.maurodata.domain.model.Pathable

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import jakarta.persistence.Transient
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item

@CompileStatic
@AutoClone
abstract class Facet extends Item implements Pathable {

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

    /**
     * Get this item's PathNode String
     */
    @Transient
    @JsonIgnore
    String getPathNodeString() {
        (new Path.PathNode(prefix: this.pathPrefix, identifier: this.pathIdentifier, modelIdentifier: this.pathModelIdentifier)).toString()
    }

    @Transient
    @JsonIgnore
    @Override
    @Nullable
    String getPathModelIdentifier() {
        return null
    }
}
