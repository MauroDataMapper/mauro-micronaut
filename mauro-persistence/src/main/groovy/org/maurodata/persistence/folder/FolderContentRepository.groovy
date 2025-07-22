package org.maurodata.persistence.folder

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.datamodel.DataModelContentRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.persistence.model.ModelContentRepository
import org.maurodata.persistence.terminology.CodeSetContentRepository
import org.maurodata.persistence.terminology.CodeSetRepository
import org.maurodata.persistence.terminology.TerminologyContentRepository
import org.maurodata.persistence.terminology.TerminologyRepository

@CompileStatic
@Singleton
class FolderContentRepository extends ModelContentRepository<Folder> {

    @Inject
    FolderRepository folderRepository

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    DataModelContentRepository dataModelContentRepository

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    TerminologyContentRepository terminologyContentRepository

    @Inject
    CodeSetRepository codeSetRepository

    @Inject
    CodeSetContentRepository codeSetContentRepository

    @Override
    Folder findWithContentById(UUID id) {
        Folder folder = folderRepository.findById(id)
        if (folder) {
            folder.dataModels = getDataModelAssociations(folder)
            folder.terminologies = getTerminologyAssociations(folder)
            folder.codeSets = getCodeSetAssociations(folder)
            folder.childFolders = surfaceChildContent(folder.childFolders)
            folder.setAssociations()
            folder
        }
    }

    @Override
    Folder saveWithContent(@NonNull Folder folder) {
        Folder saved = folderRepository.save(folder)
        super.saveAllFacets(saved)

        if (saved.childFolders) {
            saved.childFolders.each { childFolder ->
                saveWithContent(childFolder)
            }
        }

        if (saved.dataModels) {
            saved.dataModels.each{
                dataModelContentRepository.saveWithContent(it)
            }
        }
        if (saved.terminologies){
            saved.terminologies.each{
                terminologyContentRepository.saveWithContent(it)
            }
        }
        if (saved.codeSets){
            saved.codeSets.each{
                codeSetContentRepository.saveWithContent(it)
            }
        }
        saved
    }

    @Override
    Long deleteWithContent(@NonNull AdministeredItem administeredItem) {
        Folder folder = administeredItem as Folder
        folder.codeSets.each{codeSet ->
            codeSetContentRepository.deleteWithContent(codeSet)
        }

        folder.terminologies.each{terminology ->
            terminologyContentRepository.deleteWithContent(terminology)
        }

        folder.dataModels.each { dataModel ->
            dataModelContentRepository.deleteWithContent(dataModel)
        }
        folder.childFolders.each { child ->
            deleteWithContent(child)
        }
        Long result = super.deleteWithContent(folder)
        result
    }


    private List<Folder> surfaceChildContent(List<Folder> folders) {
        if (folders.isEmpty()) {
            folders
        }
        List<Folder> childFoldersWithContent = []
        folders.each {
            Folder retrieved = findWithContentById(it.id)
            retrieved.setAssociations()
            retrieved.childFolders = surfaceChildContent(retrieved.childFolders)
            childFoldersWithContent.add(retrieved)
        }
        childFoldersWithContent
    }

    private List<DataModel> getDataModelAssociations(Folder folder) {
        List<DataModel> dataModels = super.findAllModelsForFolder(dataModelRepository, folder) as List<DataModel>
        List<DataModel> dataModelsWithAssociations = []
        dataModels.each {
            DataModel retrieved = dataModelContentRepository.findWithContentById(it.id)
            retrieved.setAssociations()
            dataModelsWithAssociations.add(retrieved)
        }
        dataModelsWithAssociations
    }

    private List<Terminology> getTerminologyAssociations(Folder folder) {
        List<Terminology> terminologies = super.findAllModelsForFolder(terminologyRepository, folder) as List<Terminology>
        List<Terminology> terminologiesWithAssociations = []
        terminologies.each {
            Terminology retrieved = terminologyContentRepository.findWithContentById(it.id)
            retrieved.setAssociations()
            terminologiesWithAssociations.add(retrieved)
        }
        terminologiesWithAssociations

    }

    private List<CodeSet> getCodeSetAssociations(Folder folder) {
        List<CodeSet> codeSets = super.findAllModelsForFolder(codeSetRepository, folder) as List<CodeSet>
        List<CodeSet> codeSetsWithAssociations = []
        codeSets.each {
            CodeSet retrieved = codeSetContentRepository.readWithContentById(it.id)
            retrieved.setAssociations()
            codeSetsWithAssociations.add(retrieved)
        }
        codeSetsWithAssociations
    }
}