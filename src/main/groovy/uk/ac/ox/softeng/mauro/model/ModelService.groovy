package uk.ac.ox.softeng.mauro.model

import uk.ac.ox.softeng.mauro.tree.TreeItem

trait ModelService<M extends Model, I extends ModelItem<M>> {

    abstract List<TreeItem> buildTree(M fullTerminology, I root, Integer depth)
    abstract List<TreeItem> buildTree(M fullTerminology, I root)

    abstract Boolean handles(Class clazz)
    abstract Boolean handles(String domainType)
}
