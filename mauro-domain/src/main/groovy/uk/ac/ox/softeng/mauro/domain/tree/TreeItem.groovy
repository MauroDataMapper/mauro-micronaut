package uk.ac.ox.softeng.mauro.domain.tree

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

    Boolean isHasChildren() {
        children != null && !children.isEmpty()
    }
}
