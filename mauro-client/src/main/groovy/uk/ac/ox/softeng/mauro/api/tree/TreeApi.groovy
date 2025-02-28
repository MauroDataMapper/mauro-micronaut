package uk.ac.ox.softeng.mauro.api.tree

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue

@MauroApi
interface TreeApi {

    @Get(Paths.TREE_FOLDER)
    List<TreeItem> folderTree(@Nullable UUID id, @Nullable @QueryValue Boolean foldersOnly)

    @Get(Paths.TREE_ITEM)
    List<TreeItem> itemTree(String domainType, UUID id, @Nullable @QueryValue Boolean foldersOnly)

    @Get(Paths.TREE_ITEM_ANCESTORS)
    TreeItem itemTreeAncestors(String domainType, UUID id)

    @Get(Paths.TREE_FOLDER_ANCESTORS)
    TreeItem folderTreeAncestors(UUID id)

}
