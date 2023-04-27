package uk.ac.ox.softeng.mauro.model

import uk.ac.ox.softeng.mauro.terminology.Term
import uk.ac.ox.softeng.mauro.terminology.Terminology
import uk.ac.ox.softeng.mauro.tree.TreeItem

trait ModelService<M extends Model> {

    abstract List<TreeItem> buildTree(Terminology fullTerminology, Term root, Integer depth)
    abstract List<TreeItem> buildTree(Terminology fullTerminology, Term root)
}
