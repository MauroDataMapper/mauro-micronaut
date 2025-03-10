package uk.ac.ox.softeng.mauro.domain.tree

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model

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

    @JsonIgnore
    List<TreeItem> children

    @JsonIgnore
    AdministeredItem item

    @JsonIgnore
    Model model

    Boolean hasChildren

    UUID getModelId() {
        model?.id
    }

    /*
    Boolean getHasChildren() {
        children != null ? children : null
    }
*/
    static TreeItem from(AdministeredItem item) {
        new TreeItem(id: item.id, label: item.label, domainType: item.domainType, item: item)
    }
}
