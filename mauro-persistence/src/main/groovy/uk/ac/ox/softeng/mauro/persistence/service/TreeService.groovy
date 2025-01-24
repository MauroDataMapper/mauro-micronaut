package uk.ac.ox.softeng.mauro.persistence.service

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.domain.tree.TreeItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.CodeSetRepository

@CompileStatic
@Singleton
class TreeService {

    @Inject
    RepositoryService repositoryService

    @Inject
    FolderCacheableRepository folderCacheableRepository

    @Inject
    DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    DataClassCacheableRepository dataClassCacheableRepository

    @Inject
    AdministeredItemCacheableRepository.TermCacheableRepository termCacheableRepository

    @Inject
    ModelCacheableRepository.TerminologyCacheableRepository terminologyCacheableRepository

    @Inject
    CodeSetRepository codeSetRepository

    @CompileDynamic
    List<TreeItem> buildTree(AdministeredItem item, boolean foldersOnly, boolean setChildren) {
        switch (item) {
            case Folder -> buildTreeForFolder(item, foldersOnly, setChildren)

            case DataModel -> buildTreeForDataModel(item, setChildren)
            case DataClass -> buildTreeForDataClass(item, setChildren)

            case Terminology -> buildTreeForTerminology(item, setChildren)
            case Term -> buildTreeForTerm(item, setChildren)

            case CodeSet -> []

            default -> throw new IllegalArgumentException("Can't build tree for item of type [$item.domainType)]")
        }
    }

    List<TreeItem> buildRootFolderTree(boolean foldersOnly, boolean setChildren = true) {
        folderCacheableRepository.readAllRootFolders().collect {Folder folder ->
            TreeItem.from(folder).tap {
                if (setChildren) children = buildTree(folder, foldersOnly, false)
            }
        }
    }

    List<TreeItem> buildTreeForFolder(Folder folder, boolean foldersOnly, boolean setChildren) {
        getModelRepositories().collectMany {ModelCacheableRepository modelCacheableRepository ->
            getModelsForFolder(modelCacheableRepository, folder as Folder)
                .sort {Object model -> ((Model) model).label}
                .findAll {Object model -> !foldersOnly || ((Model) model).domainType.contains(Folder.class.simpleName)}
                .collect {Object model ->
                    TreeItem.from((Model) model).tap {
                        if (setChildren) children = buildTree(model as AdministeredItem, foldersOnly, false)
                    }
                }
        }
    }

    List<TreeItem> buildTreeForDataModel(DataModel dataModel, boolean setChildren = true) {
        List<TreeItem> treeItems = dataClassCacheableRepository.readAllByDataModelAndParentDataClassIsNull(dataModel)
            .sort {it.label}
            .collect {DataClass dataClass ->
                TreeItem.from(dataClass).tap {
                    model = dataClass.dataModel
                    if (setChildren) children = buildTree(dataClass, false, false)
                }
            }
        treeItems
    }

    List<TreeItem> buildTreeForDataClass(DataClass dataClass, boolean setChildren = true) {
        dataClassCacheableRepository.readAllByParentDataClass_Id(dataClass.id).sort {it.label}.collect {DataClass childClass ->
            TreeItem.from(childClass).tap {
                model = childClass.dataModel
                if (setChildren) children = buildTree(childClass, false, false)
            }
        }
    }

    List<TreeItem> buildTreeForTerminology(Terminology terminology, boolean setChildren = true) {
        termCacheableRepository.readChildTermsByParent(terminology.id, null).sort {it.code}.collect {Term term ->
            TreeItem.from(term).tap {
                model = term.terminology
                if (setChildren) children = buildTree(term, false, false)
            }
        }
    }

    List<TreeItem> buildTreeForTerm(Term term, boolean setChildren = true) {
        termCacheableRepository.readChildTermsByParent(term.terminology.id, term.id).sort {it.code}.collect {Term childTerm ->
            TreeItem.from(childTerm).tap {
                model = childTerm.terminology
                if (setChildren) children = buildTree(childTerm, false, false)
            }
        }
    }

    protected static List getModelsForFolder(ModelCacheableRepository modelCacheableRepository, Folder folder) {
        modelCacheableRepository.readAllByFolder(folder)
    }

    protected List<ModelCacheableRepository> getModelRepositories() {
        repositoryService.modelCacheableRepositories.sort(false) {it.class.simpleName}
    }
}
