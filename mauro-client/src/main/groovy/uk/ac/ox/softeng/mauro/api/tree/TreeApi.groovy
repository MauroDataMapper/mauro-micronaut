package uk.ac.ox.softeng.mauro.api.tree

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get

@MauroApi
interface TreeApi {

    @Get(Paths.TREE_FOLDER)
    List<TreeItem> folderTree(@Nullable UUID id)

    @Get(Paths.TREE_ITEM)
    List<TreeItem> itemTree(String domainType, UUID id)

}
