package org.maurodata.domain.tree

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

/**
 * A tree item is a component in a path to an item, which may be used as a lookup for an item, or for displaying
 * breadcrumbs on the user interface screens.
 */
@CompileStatic
@Introspected
class TreeItem {

    UUID id
    String label
    String domainType

    List<TreeItem> children

    @JsonIgnore
    AdministeredItem item

    @JsonIgnore
    Model model

    @JsonIgnore
    AdministeredItem parent

    Boolean hasChildren

    UUID getModelId() {
        model?.id
    }

    UUID getParentId() {
        parent?.id
    }

    List<String> availableActions = []

    String modelVersion
    String modelVersionTag
    String path
    String branchName

    Boolean getFinalised() {
        if (!(item instanceof Model)) {return null}
        return ((Model) item).finalised
    }

    static TreeItem from(AdministeredItem item) {
        new TreeItem(id: item.id, label: item.label, domainType: item.domainType, item: item, availableActions: new ArrayList<String>(item.availableActions),
                     path: item.updatePath().toString(), model: item.getOwner(), parent: item.getParent(),
                     branchName: (item instanceof Model)? item.branchName : null,
                     modelVersion: (item instanceof Model)? item.modelVersion : null,
                     modelVersionTag: (item instanceof Model)? item.modelVersionTag : null
                     )
    }
}
