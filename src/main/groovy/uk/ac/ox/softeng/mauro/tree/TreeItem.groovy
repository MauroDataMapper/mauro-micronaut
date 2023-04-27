package uk.ac.ox.softeng.mauro.tree

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected

@CompileStatic
@Introspected
class TreeItem {

    UUID id
    String label
    String domainType
    Boolean hasChildren

    List<TreeItem> children

    Boolean getHasChildren() {
        children != null && !children.isEmpty()
    }
}
