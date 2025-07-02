package org.maurodata.domain.diff

trait DiffableItem<T extends DiffableItem> {
    abstract CollectionDiff fromItem()

    abstract String getDiffIdentifier()

    abstract ObjectDiff<T> diff(T other, String lhsPathRoot, String rhsPathRoot)


}