package org.maurodata.domain.diff

import groovy.transform.CompileStatic

@CompileStatic
trait DiffableItem<T extends DiffableItem> {
    abstract CollectionDiff fromItem()

    abstract String getDiffIdentifier()

    abstract ObjectDiff<T> diff(T other, String lhsPathRoot, String rhsPathRoot)


}