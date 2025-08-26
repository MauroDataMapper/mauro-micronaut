package org.maurodata.domain.facet

import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient

@CompileStatic
@MappedEntity(schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'], unique = true)])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class SemanticLink extends Facet implements ItemReferencer {

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

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'sl'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        "${linkType}.${targetMultiFacetAwareItemDomainType}.${targetMultiFacetAwareItemId}"
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> getItemReferences() {
        List<ItemReference> pathsBeingReferenced = []
        if (targetMultiFacetAwareItemId != null) {
            pathsBeingReferenced << ItemReference.from(targetMultiFacetAwareItemId,targetMultiFacetAwareItemDomainType)
        }
        return pathsBeingReferenced
    }

    @Override
    void replaceItemReferences(Map<UUID, ItemReference> replacements) {
        if (targetMultiFacetAwareItemId != null) {
            ItemReference replacementItemReference = replacements.get(targetMultiFacetAwareItemId)
            if (replacementItemReference != null) {
                targetMultiFacetAwareItemId = replacementItemReference.itemId
            }
        }
    }
}