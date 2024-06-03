package uk.ac.ox.softeng.mauro.persistence.folder

import groovy.transform.CompileStatic
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
            folder.dataModels = setDataModelAssociations(folder)
            folder.terminologies = setTerminologyAssociations(folder)
            folder.codeSets = setCodeSetAssociations(folder)
            folder.setAssociations()
            folder
        }
    }

    private List<DataModel> setDataModelAssociations(Folder folder) {
        List<DataModel> dataModels = super.findAllModelsForFolder(dataModelRepository, folder) as List<DataModel>
        List<DataModel> dataModelsWithAssociations = []
        dataModels.each {
            DataModel retrieved = dataModelContentRepository.findWithContentById(it.id)
            retrieved.setAssociations()
            dataModelsWithAssociations.add(retrieved)
        }
        dataModelsWithAssociations
    }

    private List<Terminology> setTerminologyAssociations(Folder folder) {
        List<Terminology> terminologies = super.findAllModelsForFolder(terminologyRepository, folder) as List<Terminology>
        List<Terminology> terminologiesWithAssociations = []
        terminologies.each {
            Terminology retrieved = terminologyContentRepository.findWithContentById(it.id)
            retrieved.setAssociations()
            terminologiesWithAssociations.add(retrieved)
        }
        terminologiesWithAssociations

    }

    private List<CodeSet> setCodeSetAssociations(Folder folder) {
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