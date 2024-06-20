package uk.ac.ox.softeng.mauro.persistence.folder

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton



import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.CodeSetContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.CodeSetRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository

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