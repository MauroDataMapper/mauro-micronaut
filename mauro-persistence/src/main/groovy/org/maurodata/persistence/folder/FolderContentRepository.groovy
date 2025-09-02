package org.maurodata.persistence.folder

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.model.ModelContentRepository
import org.maurodata.persistence.model.ModelRepository

@CompileStatic
@Singleton
class FolderContentRepository extends ModelContentRepository<Folder> {

    ModelCacheableRepository.FolderCacheableRepository folderRepository

    List<ModelContentRepository> modelContentRepositories

    List<ModelRepository> modelRepositories

    @Inject
    FolderContentRepository(  ModelCacheableRepository.FolderCacheableRepository folderCacheableRepository,
                              List<ModelContentRepository> modelContentRepositories, List<ModelRepository> modelRepositories) {
        this.folderRepository = folderCacheableRepository
        this.modelRepositories = modelRepositories.findAll {it !instanceof ItemCacheableRepository}
        this.modelContentRepositories = modelContentRepositories
    }

    @Override
    Folder findWithContentById(UUID id) {
        Folder folder = folderRepository.findById(id)
        if (folder) {
            folder.dataModels = getModelAssociations(folder, DataModel.class) as List<DataModel>
            folder.terminologies = getModelAssociations(folder, Terminology.class) as List<Terminology>
            folder.codeSets = getModelAssociations(folder, CodeSet.class) as List<CodeSet>
            folder.classificationSchemes = getModelAssociations(folder, ClassificationScheme.class) as List<ClassificationScheme>
            folder.childFolders = surfaceChildContent(folder.childFolders)
        }
        folder
    }

    @Override
    Folder saveWithContent(@NonNull Folder folder) {
        Folder saved = folderRepository.save(folder)
        super.saveAllFacets(saved)

        if (saved.childFolders) {
            saved.childFolders.each {childFolder ->
                saveWithContent(childFolder)
            }
        }
        if (saved.classificationSchemes) {
            saved.classificationSchemes.each {
                getModelContentRepository(it.class).saveWithContent(it)
            }
        }

        if (saved.terminologies) {
            saved.terminologies.each {
                getModelContentRepository(it.class).saveWithContent(it)
            }
        }
        if (saved.codeSets) {
            saved.codeSets.each {
                getModelContentRepository(it.class).saveWithContent(it)
            }
        }
        if (saved.dataModels) {
            saved.dataModels.each {
                getModelContentRepository(it.class).saveWithContent(it)
            }
        }
        saved
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        Folder folder = administeredItem as Folder
        folder.childFolders.each {child ->
            deleteWithContent(child)
        }
        folder.codeSets.each {codeSet ->
            getModelContentRepository(CodeSet.class).deleteWithContent(codeSet)
        }
        folder.terminologies.each {terminology ->
            getModelContentRepository(Terminology.class).deleteWithContent(terminology)
        }
        folder.dataModels.each {dataModel ->
            getModelContentRepository(DataModel.class).deleteWithContent(dataModel)
        }
        folder.classificationSchemes.each {classificationScheme ->
            getModelContentRepository(ClassificationScheme.class).deleteWithContent(classificationScheme)
        }
        Long result = super.deleteWithContent(folder)
        result
    }


    protected ModelContentRepository getModelContentRepository(Class clazz) {
        modelContentRepositories.find {
            it.getClass().simpleName != 'ModelContentRepository' &&
            it.handles(clazz)
        } ?:
        modelContentRepositories.find {
            it.getClass().simpleName == 'ModelContentRepository'
        }
    }

    protected List<Folder> surfaceChildContent(List<Folder> folders) {
        if (folders.isEmpty()) {
            return folders
        }
        folders.collect {
            Folder retrieved = findWithContentById(it.id)
            retrieved.childFolders = surfaceChildContent(retrieved.childFolders)
            retrieved
        }
    }

    private List<Model> getModelAssociations(Folder folder, Class clazz) {
        ModelRepository modelRepository = modelRepositories.find {it.handles(clazz.simpleName)}
        ModelContentRepository modelContentRepository = getModelContentRepository(clazz)

        List<Model> models = super.findAllModelsForFolder(modelRepository, folder) as List<Model>
        models.collect{
            Model retrieved = modelContentRepository.findWithContentById((it as Model).id)
            retrieved
        }

    }
}