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
    List<TreeItem> buildTree(AdministeredItem item, boolean setHasChildren = true) {
        switch (item) {
            case Folder -> buildTreeForFolder(item, setHasChildren)

            case DataModel -> buildTreeForDataModel(item, setHasChildren)
            case DataClass -> buildTreeForDataClass(item, setHasChildren)

            case Terminology -> buildTreeForTerminology(item, setHasChildren)
            case Term -> buildTreeForTerm(item, setHasChildren)

            default -> throw new IllegalArgumentException("Can't get children for item of type [$item.domainType)]")
        }
    }

    List<TreeItem> buildTreeForFolder(Folder folder, boolean setHasChildren = true) {
        repositoryService.modelCacheableRepositories.collectMany {ModelCacheableRepository modelCacheableRepository ->
            modelCacheableRepository.readAllByFolder(folder).collect {Model model ->
                TreeItem.from(model).tap {
                    if (setHasChildren) hasChildren = buildTree(model, false)
                }
            }
        }
    }

    List<TreeItem> buildTreeForDataModel(DataModel dataModel, boolean setHasChildren = true) {
        dataClassCacheableRepository.readAllByParent(dataModel).collect {DataClass dataClass ->
            TreeItem.from(dataClass).tap {
                if (setHasChildren) hasChildren = buildTree(dataClass, false)
            }
        }
    }

    List<TreeItem> buildTreeForDataClass(DataClass dataClass, boolean setHasChildren = true) {
        dataClassCacheableRepository.readAllByParentDataClass_Id(dataClass.id).collect {DataClass childClass ->
            TreeItem.from(childClass).tap {
                if (setHasChildren) hasChildren = buildTree(childClass, false)
            }
        }
    }

    List<TreeItem> buildTreeForTerminology(Terminology terminology, boolean setHasChildren = true) {
        termCacheableRepository.readChildTermsByParent(terminology.id, null).collect {Term term ->
            TreeItem.from(term).tap {
                if (setHasChildren) hasChildren = buildTree(term, false)
            }
        }
    }

    List<TreeItem> buildTreeForTerm(Term term, boolean setHasChildren = true) {
        termCacheableRepository.readChildTermsByParent(term.terminology.id, term.id).collect {Term childTerm ->
            TreeItem.from(term).tap {
                if (setHasChildren) hasChildren = buildTree(childTerm, false)
            }
        }
    }

//    List<TreeItem> buildTreeForCodeSet(CodeSet codeSet, boolean setHasChildren = true) {
//        codeSetRepository.getTerms(codeSet.id).collect {Term term ->
//            TreeItem.from(term)
//        }
//    }

//    boolean hasChildren(Model model) {
//        switch (model) {
//            case Folder -> hasChildren((Folder) model)
//            case DataModel -> hasChildren((DataModel) model)
//            case Terminology -> hasChildren((Terminology) model)
//            case CodeSet -> hasChildren((CodeSet) model)
//            default -> throw new IllegalArgumentException("Can't get children for model of type [$model.modelType)]")
//        }
//    }
//
//    boolean hasChildren(Folder folder) {
//        repositoryService.modelCacheableRepositories.any {ModelCacheableRepository repository ->
//            repository.readAllByFolder(folder)
//        }
//    }
//
//    boolean hasChildren(DataModel dataModel) {
//        dataClassCacheableRepository.readAllByParent(dataModel)
//    }
//
//    boolean hasChildren(Terminology terminology) {
//        termCacheableRepository.readAllByParent(terminology)
//    }
//
//    boolean hasChildren(CodeSet codeSet) {
//        codeSetRepository.getTerms(codeSet.id)
//    }
}
