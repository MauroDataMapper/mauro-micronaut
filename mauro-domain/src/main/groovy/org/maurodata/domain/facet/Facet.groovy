package org.maurodata.domain.facet

import groovy.util.logging.Slf4j
import io.micronaut.data.annotation.MappedEntity
import jakarta.persistence.Entity
import jakarta.persistence.PrePersist
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils
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
@Slf4j
@MappedEntity(schema = 'core')
@AutoClone(excludes = ['multiFacetAwareItem'])
abstract class Facet extends Item implements Pathable, ItemReferencer {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias(['multi_facet_aware_item_domain_type'])
    String multiFacetAwareItemDomainType

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonAlias(['multi_facet_aware_item_id'])
    UUID multiFacetAwareItemId

    @Transient
    @JsonIgnore
    AdministeredItem multiFacetAwareItem

    @PrePersist
    void prePersist() {
        if(multiFacetAwareItem && !multiFacetAwareItemId) {
            multiFacetAwareItemId = multiFacetAwareItem.id
            multiFacetAwareItemDomainType = multiFacetAwareItem.domainType
        } else {
            log.error("Trying to save Facet without 'multiFacetAwareItem' set")
            log.error("" + multiFacetAwareItem)
            log.error("" + multiFacetAwareItemId)

        }
    }

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

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Facet intoFacet = (Facet) into
        intoFacet.multiFacetAwareItemDomainType = ItemUtils.copyItem(this.multiFacetAwareItemDomainType, intoFacet.multiFacetAwareItemDomainType)
        intoFacet.multiFacetAwareItemId = ItemUtils.copyItem(this.multiFacetAwareItemId, intoFacet.multiFacetAwareItemId)
        intoFacet.multiFacetAwareItem = ItemUtils.copyItem(this.multiFacetAwareItem, intoFacet.multiFacetAwareItem)
        intoFacet.domainType = ItemUtils.copyItem(this.domainType, intoFacet.domainType)
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItem(multiFacetAwareItem, pathsBeingReferenced)
        ItemReferencerUtils.addIdType(multiFacetAwareItemId, multiFacetAwareItemDomainType, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)
        multiFacetAwareItem = ItemReferencerUtils.replaceItemByIdentity(multiFacetAwareItem, replacements, notReplaced)
        // Can't do this yet
        //multiFacetAwareItemId = ItemReferencerUtils.replaceIdTypeByIdentity(multiFacetAwareItemId, replacements)
    }
}
