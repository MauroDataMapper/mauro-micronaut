package uk.ac.ox.softeng.mauro.tree

import uk.ac.ox.softeng.mauro.model.Model
import uk.ac.ox.softeng.mauro.model.ModelItem
import uk.ac.ox.softeng.mauro.model.ModelService

import jakarta.inject.Inject

class TreeItemService {

    @Inject
    List<ModelService> modelServices

    List<TreeItem> buildTree(Model fullModel, ModelItem root, Integer depth = null) {
        ModelService modelService = modelServices.find {it.handles(fullModel.class)}
        modelService.buildTree(fullModel, root, depth)
    }
}
