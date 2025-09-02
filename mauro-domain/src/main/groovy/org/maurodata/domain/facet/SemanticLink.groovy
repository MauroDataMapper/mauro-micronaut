package org.maurodata.domain.facet

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

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
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addIdType(targetMultiFacetAwareItemId, targetMultiFacetAwareItemDomainType, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)
        // Can't do this by Item
        // targetMultiFacetAwareItemId = ItemReferencerUtils.replaceIdTypeByIdentity(targetMultiFacetAwareItemId, replacements)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        SemanticLink intoSemanticLink = (SemanticLink) into

        intoSemanticLink.linkType = ItemUtils.copyItem(this.linkType, intoSemanticLink.linkType)
        intoSemanticLink.targetMultiFacetAwareItemId = ItemUtils.copyItem(this.targetMultiFacetAwareItemId, intoSemanticLink.targetMultiFacetAwareItemId)
        intoSemanticLink.targetMultiFacetAwareItemDomainType =
            ItemUtils.copyItem(this.targetMultiFacetAwareItemDomainType, intoSemanticLink.targetMultiFacetAwareItemDomainType)
        intoSemanticLink.unconfirmed = ItemUtils.copyItem(this.unconfirmed, intoSemanticLink.unconfirmed)
    }

    @Override
    Item shallowCopy() {
        SemanticLink semanticLinkShallowCopy = new SemanticLink()
        this.copyInto(semanticLinkShallowCopy)
        return semanticLinkShallowCopy
    }
}