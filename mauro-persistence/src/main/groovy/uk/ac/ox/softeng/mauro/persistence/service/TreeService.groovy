package uk.ac.ox.softeng.mauro.persistence.service

import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier

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
import uk.ac.ox.softeng.mauro.persistence.model.PathRepository
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

    @Inject
    AdministeredItemCacheableRepository.ClassifierCacheableRepository classifierCacheableRepository

    @Inject
    PathRepository pathRepository

    @CompileDynamic
    List<TreeItem> buildTree(AdministeredItem item, boolean foldersOnly, boolean setChildren, boolean lookForChildren) {

        pathRepository.readParentItems(item)
        item.updatePath()
        item.updateBreadcrumbs()

        switch (item) {
            case Folder -> buildTreeForFolder(item, foldersOnly, setChildren, lookForChildren)

            case DataModel -> buildTreeForDataModel(item, setChildren, lookForChildren)
            case DataClass -> buildTreeForDataClass(item, setChildren, lookForChildren)

            case Terminology -> buildTreeForTerminology(item, setChildren, lookForChildren)
            case Term -> buildTreeForTerm(item, setChildren, lookForChildren)

            case CodeSet -> []

            case ClassificationScheme -> buildTreeForClassificationScheme(item, setChildren, lookForChildren)
            case Classifier -> buildTreeForClassifier(item, setChildren, lookForChildren)

            default -> throw new IllegalArgumentException("Can't build tree for item of type [$item.domainType)]")
        }
    }

    List<TreeItem> buildRootFolderTree(boolean foldersOnly) {
        folderCacheableRepository.readAllRootFolders().collect {Folder folder ->

            pathRepository.readParentItems(folder)
            folder.updatePath()
            folder.updateBreadcrumbs()

            TreeItem.from(folder).tap {
                List<TreeItem> theChildren = buildTree(folder, foldersOnly, false, false)
                hasChildren = theChildren && theChildren.size() > 0
                children = []
            }
        }
    }

    List<TreeItem> buildTreeForFolder(Folder folder, boolean foldersOnly, boolean setChildren, boolean lookForChildren) {
        pathRepository.readParentItems(folder)
        folder.updatePath()
        folder.updateBreadcrumbs()

        List<TreeItem> treeItems = []
        List<Model> models = new ArrayList<>()

        getModelRepositories().forEach {ModelCacheableRepository modelCacheableRepository ->
            models.addAll(getModelsForFolder(modelCacheableRepository, folder as Folder))
        }

        models.sort {Model model -> model.label}
            .findAll {Model model -> !foldersOnly || model.domainType.contains(Folder.class.simpleName)}
            .collect {Model model ->

                pathRepository.readParentItems(model)
                model.updatePath()
                model.updateBreadcrumbs()

                TreeItem treeItem = TreeItem.from(model).tap {

                    List<TreeItem> theChildren
                    if (lookForChildren) {
                        theChildren = buildTree(model as AdministeredItem, foldersOnly, false, setChildren)
                        if (setChildren) {children = theChildren} else {children = []}
                        hasChildren = theChildren && theChildren.size() > 0
                    } else {
                        children = []
                    }
                }
                treeItems.add(treeItem)
            }

        return treeItems
    }

    TreeItem buildTreeItemForThis(AdministeredItem administeredItem, boolean foldersOnly, boolean setChildren, boolean lookForChildren) {
        pathRepository.readParentItems(administeredItem)
        administeredItem.updatePath()
        administeredItem.updateBreadcrumbs()

        TreeItem treeItem = TreeItem.from(administeredItem).tap {

            List<TreeItem> theChildren
            if (lookForChildren) {
                theChildren = buildTree(administeredItem as AdministeredItem, foldersOnly, false, setChildren)
                if (setChildren) {children = theChildren} else {children = []}
                hasChildren = theChildren && theChildren.size() > 0
            } else {
                children = []
            }
        }

        return treeItem
    }

    List<TreeItem> buildTreeForDataModel(DataModel dataModel, boolean setChildren, boolean lookForChildren) {
        List<TreeItem> treeItems = dataClassCacheableRepository.readAllByDataModelAndParentDataClassIsNull(dataModel)
            .sort {it.label}
            .collect {DataClass dataClass ->
                TreeItem.from(dataClass).tap {
                    model = dataClass.dataModel
                    List<TreeItem> theChildren
                    if (lookForChildren) {
                        theChildren = buildTree(dataClass, false, false, setChildren)
                        if (setChildren) {children = theChildren} else {children = []}
                        hasChildren = theChildren && theChildren.size() > 0
                    } else {
                        children = []
                    }
                }
            }
        treeItems
    }

    List<TreeItem> buildTreeForDataClass(DataClass dataClass, boolean setChildren, boolean lookForChildren) {
        dataClassCacheableRepository.readAllByParentDataClass_Id(dataClass.id).sort {it.label}.collect {DataClass childClass ->
            TreeItem.from(childClass).tap {
                model = childClass.dataModel

                List<TreeItem> theChildren
                if (lookForChildren) {
                    theChildren = buildTree(childClass, false, false, setChildren)
                    if (setChildren) {children = theChildren} else {children = []}
                    hasChildren = theChildren && theChildren.size() > 0
                } else {
                    children = []
                }
            }
        }
    }

    List<TreeItem> buildTreeForTerminology(Terminology terminology, boolean setChildren, boolean lookForChildren) {
        termCacheableRepository.readChildTermsByParent(terminology.id, null).sort {it.code}.collect {Term term ->
            TreeItem.from(term).tap {
                model = term.terminology

                List<TreeItem> theChildren
                if (lookForChildren) {
                    theChildren = buildTree(term, false, false, setChildren)
                    if (setChildren) {children = theChildren} else {children = []}
                    hasChildren = theChildren && theChildren.size() > 0
                } else {
                    children = []
                }
            }
        }
    }

    List<TreeItem> buildTreeForTerm(Term term, boolean setChildren, boolean lookForChildren) {
        termCacheableRepository.readChildTermsByParent(term.terminology.id, term.id).sort {it.code}.collect {Term childTerm ->
            TreeItem.from(childTerm).tap {
                model = childTerm.terminology
                List<TreeItem> theChildren
                if (lookForChildren) {
                    theChildren = buildTree(childTerm, false, false, setChildren)
                    if (setChildren) {children = theChildren} else {children = []}
                    hasChildren = theChildren && theChildren.size() > 0
                } else {
                    children = []
                }
            }
        }
    }

    List<TreeItem> buildTreeForClassificationScheme(ClassificationScheme classificationScheme, boolean setChildren, boolean lookForChildren) {
        List<TreeItem> treeItems =
            classifierCacheableRepository.readAllByClassificationScheme_Id(classificationScheme.id).sort {it.label}.collect {Classifier childClassifier ->
                TreeItem.from(childClassifier).tap {
                    model = childClassifier.classificationScheme
                    List<TreeItem> theChildren
                    if (lookForChildren) {
                        theChildren = buildTree(childClassifier, false, false, setChildren)
                        if (setChildren) {children = theChildren} else {children = []}
                        hasChildren = theChildren && theChildren.size() > 0
                    } else {
                        children = []
                    }
                }
            }
        treeItems
    }

    List<TreeItem> buildTreeForClassifier(Classifier classifier, boolean setChildren, boolean lookForChildren) {
        classifierCacheableRepository.readAllByParentClassifier_Id(classifier.id).sort {it.label}.collect {Classifier child ->
            TreeItem.from(child).tap {
                model = child.classificationScheme
                List<TreeItem> theChildren
                if (lookForChildren) {
                    theChildren = buildTree(child, false, false, setChildren)
                    if (setChildren) {children = theChildren} else {children = []}
                    hasChildren = theChildren && theChildren.size() > 0
                } else {
                    children = []
                }
            }
        }
    }

    protected static List<Model> getModelsForFolder(ModelCacheableRepository modelCacheableRepository, Folder folder) {
        modelCacheableRepository.readAllByFolder(folder)
    }

    protected List<ModelCacheableRepository> getModelRepositories() {
        repositoryService.modelCacheableRepositories.sort(false) {it.class.simpleName}
    }
}
