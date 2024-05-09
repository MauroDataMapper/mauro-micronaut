package uk.ac.ox.softeng.mauro.domain.diff

trait DiffableItem<T extends DiffableItem> {
    abstract CollectionDiff fromItem()

    abstract String getDiffIdentifier()

    abstract ObjectDiff diff(T other)

//    abstract Integer getNumberOfDiffs()
}