package org.maurodata.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SemanticLink extends Facet {

    @JsonAlias(['link_type'])
    SemanticLinkType linkType
    @JsonAlias(['target_multi_facet_aware_item_id'])
    UUID targetMultiFacetAwareItemId
    @JsonAlias(['target_multi_facet_aware_item_domain_type'])
    String targetMultiFacetAwareItemDomainType
    Boolean unconfirmed

    SemanticLink() {
        unconfirmed = false
    }
}