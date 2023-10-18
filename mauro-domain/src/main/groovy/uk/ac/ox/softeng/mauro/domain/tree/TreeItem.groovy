package uk.ac.ox.softeng.mauro.domain.tree

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
    Boolean hasChildren

    List<TreeItem> children

    Boolean isHasChildren() {
        children != null && !children.empty
    }
}
