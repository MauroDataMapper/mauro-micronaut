package org.maurodata.domain.facet

import groovy.util.logging.Slf4j
import jakarta.persistence.PrePersist
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
import io.micronaut.data.annotation.*
import org.maurodata.domain.model.Model

@CompileStatic
@MappedEntity(value = 'version_link', schema = 'core')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
@Slf4j
class VersionLink extends Facet implements ItemReferencer {

    final static String NEW_FORK_OF = "NEW_FORK_OF", NEW_MODEL_VERSION_OF = "NEW_MODEL_VERSION_OF"
    final static Map<String, String> descriptions = [:]
    static {
        descriptions.put(NEW_FORK_OF, "New Fork Of")
        descriptions.put(NEW_MODEL_VERSION_OF, "New Model Version Of")
    }

    @JsonIgnore
    @Transient
    Model target

    @JsonAlias(['version_link_type'])
    String versionLinkType

    @JsonAlias(['target_model_id'])
    UUID targetModelId

    @JsonAlias(['target_model_domain_type'])
    String targetModelDomainType

    @PrePersist
    void prePersist() {
        super.prePersist()
        if(target) {
            if(!target.id) {
                log.error("Trying to save a version link with a target which doesn't have an id!")
            } else {
                targetModelDomainType = target.domainType
                targetModelId = target.id
            }
        }
    }

    /****
     * Methods for building a tree-like DSL
     */

    static VersionLink build(
        Map args,
        @DelegatesTo(value = VersionLink, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new VersionLink(args).tap(closure)
    }

    static VersionLink build(
        @DelegatesTo(value = VersionLink, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    String setVersionLinkType(final String linkType) {
        this.versionLinkType = linkType
        this.versionLinkType
    }

    @JsonIgnore
    @Transient
    String getDescription() {
        if (versionLinkType == null) {return ""}

        final String description = descriptions.get(versionLinkType)
        if (description != null) {return description}

        return ""
    }

    @JsonIgnore
    @Transient
    Model setTargetModel(final Model targetModel) {
        this.targetModelId = targetModel.id
        this.targetModelDomainType = targetModel.domainType
        targetModel
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'vl'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        "${versionLinkType}.${targetModelDomainType}.${targetModelId}"
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()
        ItemReferencerUtils.addIdType(targetModelId, targetModelDomainType, pathsBeingReferenced)
        return pathsBeingReferenced
    }

    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)
        // The targetModelId shouldn't actually be changed, should it?
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        VersionLink intoVersionLink = (VersionLink) into
        intoVersionLink.versionLinkType = ItemUtils.copyItem(this.versionLinkType, intoVersionLink.versionLinkType)
        intoVersionLink.targetModelId = ItemUtils.copyItem(this.targetModelId, intoVersionLink.targetModelId)
        intoVersionLink.targetModelDomainType = ItemUtils.copyItem(this.targetModelDomainType, intoVersionLink.targetModelDomainType)
    }

    @Override
    Item shallowCopy() {
        VersionLink versionLinkShallowCopy = new VersionLink()
        this.copyInto(versionLinkShallowCopy)
        return versionLinkShallowCopy
    }
}
