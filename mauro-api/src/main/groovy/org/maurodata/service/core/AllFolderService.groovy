package org.maurodata.service.core

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.folder.FolderContentRepository
import org.maurodata.security.AccessControlService
import org.maurodata.service.datamodel.DataModelImportService

@CompileStatic
@Slf4j
@Singleton
class AllFolderService extends AdministeredItemService {

    @Inject
    DataModelImportService dataModelImportService

    @Inject
    AccessControlService accessControlService

    List<Folder> importModel(List<Folder> exported, FolderContentRepository folderContentRepository) {
        exported.collect {imp ->
            imp.dataModels.each {dataModelImp ->
                dataModelImp = dataModelImportService.preProcessDataFlows(dataModelImp)
            }

            imp = updateCreationProperties(imp)
            imp.catalogueUser = accessControlService.getUser() ?: null
            log.info '** about to saveWithContentBatched... **'
            Folder savedImported = folderContentRepository.saveWithContent(imp as Folder)

            savedImported.dataModels = dataModelImportService.saveDataFlowModelItems(savedImported.dataModels)
            log.info '** finished saveWithContentBatched **'
            savedImported
        }
    }
}
