package uk.ac.ox.softeng.mauro.domain.tree


import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem
import uk.ac.ox.softeng.mauro.domain.model.ModelService

class TreeItemService {

    @Inject
    List<ModelService> modelServices

    List<TreeItem> buildTree(Model fullModel, ModelItem root, Integer depth = null) {
        ModelService modelService = modelServices.find {it.handles(fullModel.class)}
        modelService.buildTree(fullModel, root, depth)
    }
}
