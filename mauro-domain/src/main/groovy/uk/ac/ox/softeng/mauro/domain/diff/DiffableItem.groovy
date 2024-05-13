package uk.ac.ox.softeng.mauro.domain.diff

trait DiffableItem<T extends DiffableItem> {
    abstract CollectionDiff fromItem()

    abstract String getDiffIdentifier()

    abstract ObjectDiff<T> diff(T other)


}