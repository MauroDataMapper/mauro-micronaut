package uk.ac.ox.softeng.mauro.plugin.importer

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModelService
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.importer.ImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
class JsonDataModelImporterPlugin implements ModelImporterPlugin<DataModel, ImportParameters> {

    String version = '4.0.0'

    String displayName = 'JSON DataModel Importer'

    Boolean canImportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper


    @Override
    DataModel importDomain(ImportParameters params) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(params.importFile, ExportModel)
        log.info '*** imported JSON model ***'
        DataModel imported = importModel.dataModel
        imported.setAssociations()

        updateCreationProperties(imported)
        log.info '* start updateCreationProperties *'
        imported.getAllContents().each {updateCreationProperties(it)}
        log.info '* finish updateCreationProperties *'

        UUID folderId = UUID.fromString(importMap.folderId)

        Folder folder = folderRepository.readById(folderId)
        imported.folder = folder
        log.info '** about to saveWithContentBatched... **'
        DataModel savedImported = modelContentRepository.saveWithContent(imported)
        log.info '** finished saveWithContentBatched **'
        ListResponse.from([show(savedImported.id)])

    }

    @Override
    List<DataModel> importDomains(ImportParameters params) {
        return null
    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.datamodel+json'
    }
}
