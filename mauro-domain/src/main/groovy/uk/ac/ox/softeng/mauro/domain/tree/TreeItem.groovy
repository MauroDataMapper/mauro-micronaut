package uk.ac.ox.softeng.mauro.domain.tree

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item

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
    Boolean hasChildren

    List<TreeItem> children

    // Attach the original item to the TreeItem, to make security checks easier.
    @JsonIgnore
    Item item

//    Boolean isHasChildren() {
//        children != null && !children.empty
//    }

    static TreeItem from(AdministeredItem item) {
        new TreeItem(id: item.id, label: item.label, domainType: item.domainType, item: item)
    }

//    static TreeItem from(Folder folder) {
//        new TreeItem(id: folder.id, label: folder.label, domainType: folder.domainType)
//    }
//
//    static TreeItem from(DataModel dataModel) {
//        new TreeItem(id: dataModel.id, label: dataModel.label, domainType: dataModel.domainType)
//    }
}
