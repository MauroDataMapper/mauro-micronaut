package org.maurodata.plugin.importer.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Terminology
import org.maurodata.export.ExportModel
import org.maurodata.plugin.JsonPluginConstants
import org.maurodata.plugin.importer.FileImportParameters
import org.maurodata.plugin.importer.FolderImporterPlugin

@Slf4j
@Singleton
@CompileStatic
class JsonFolderImporterPlugin implements FolderImporterPlugin<FileImportParameters> {

    String version = JsonPluginConstants.VERSION

    String displayName = 'JSON Folder Importer'

    Boolean canImportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    List<Folder> importDomain(FileImportParameters params) {
        log.info '** start importModel **'

        JsonNode importModelTree = objectMapper.readTree(params.importFile.fileContents)

        Map<UUID, JsonNode> folderNodesMap = [:]
        if (importModelTree.has('folder')) {
            addAllFoldersToMap(importModelTree.get('folder'), folderNodesMap)
        } else if (importModelTree.has('folders')) {
            importModelTree.get('folders').toList().each {addAllFoldersToMap(it, folderNodesMap)}
        }
        ExportModel importModel = objectMapper.treeToValue(importModelTree, ExportModel)

        Map<UUID, Folder> foldersMap = [:]
        if (importModel.folder) {
            addAllFoldersToMap(importModel.folder, foldersMap)
        } else if (importModel.folders) {
            importModel.folders.each {addAllFoldersToMap(it, foldersMap)}
        }

        // Import each Terminology as a separate object
        foldersMap.each {UUID folderId, Folder folder ->
            folder.terminologies = readTerminologiesInFolder(folderNodesMap[folderId])
        }

        log.info '*** imported JSON model ***'

        if (importModel.folder) {
            return [importModel.folder]
        } else {
            return importModel.folders ?: []
        }
    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'application/mauro.folder+json'
    }

    @Override
    Class<FileImportParameters> importParametersClass() {
        return FileImportParameters
    }

    List<Terminology> readTerminologiesInFolder(JsonNode folder) {
        List<JsonNode> terminologyJsonNodes = folder.at('/terminologies').asList()
        terminologyJsonNodes.collect {objectMapper.treeToValue(it, Terminology)}
    }

    void addAllFoldersToMap(JsonNode folder, Map<UUID, JsonNode> foldersMap) {
        // if the folder does not have an ID, assign a random one now as Terminologies are mapped to folder by ID
        if (!folder.has('id')) {
            ((ObjectNode) folder).put('id', UUID.randomUUID().toString())
        }
        foldersMap[UUID.fromString(folder.get('id').asText())] = folder
        folder.get('childFolders')?.asList()?.each {addAllFoldersToMap(it, foldersMap)}
    }

    void addAllFoldersToMap(Folder folder, Map<UUID, Folder> foldersMap) {
        foldersMap[folder.id] = folder
        folder.childFolders.each {addAllFoldersToMap(it, foldersMap)}
    }
}