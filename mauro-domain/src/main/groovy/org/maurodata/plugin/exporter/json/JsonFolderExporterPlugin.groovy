package org.maurodata.plugin.exporter.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.export.ExportModel
import org.maurodata.plugin.JsonPluginConstants
import org.maurodata.plugin.exporter.FolderExporterPlugin

@Slf4j
@Singleton
@CompileStatic
class JsonFolderExporterPlugin implements FolderExporterPlugin {

    String version = JsonPluginConstants.VERSION

    String displayName = 'JSON Folder Exporter'

    Boolean canExportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    byte[] exportModel(Folder folder) {
        Map<UUID, Folder> foldersMap = [:]
        addAllFoldersToMap(folder, foldersMap)

        ExportModel exportModel = new ExportModel(this)
        exportModel.folder = folder

        Map<UUID, JsonNode> folderNodesMap = [:]
        JsonNode exportModelNode = objectMapper.valueToTree(exportModel)
        addAllFoldersToMap(exportModelNode.get('folder'), folderNodesMap)

        // Export each Terminology as a separate object
        foldersMap.each {UUID folderId, Folder f ->
            List<JsonNode> terminologyNodes = f.terminologies.collect {objectMapper.valueToTree(it)}
            ((ObjectNode) folderNodesMap[folderId]).putArray('terminologies').addAll(terminologyNodes)
        }

        objectMapper.writeValueAsBytes(exportModelNode)
    }

    @Override
    String getFileExtension() {
        return ".json"
    }

    @Override
    String getFileName(Folder model) {
        return model.label + ".json"
    }

    @Override
    byte[] exportModels(Collection<Folder> folders) {
        Map<UUID, Folder> foldersMap = [:]
        folders.each {addAllFoldersToMap(it, foldersMap)}

        ExportModel exportModel = new ExportModel(this)
        exportModel.folders.addAll(folders)

        Map<UUID, JsonNode> folderNodesMap = [:]
        JsonNode exportModelNode = objectMapper.valueToTree(exportModel)
        exportModelNode.get('folders')?.asList()?.each {addAllFoldersToMap(it, folderNodesMap)}

        // Export each Terminology as a separate object
        foldersMap.each {UUID folderId, Folder f ->
            List<JsonNode> terminologyNodes = f.terminologies.collect {objectMapper.valueToTree(it)}
            ((ObjectNode) folderNodesMap[folderId]).putArray('terminologies').addAll(terminologyNodes)
        }

        objectMapper.writeValueAsBytes(exportModel)

    }

    void addAllFoldersToMap(JsonNode folder, Map<UUID, JsonNode> foldersMap) {
        foldersMap[UUID.fromString(folder.get('id').asText())] = folder
        folder.get('childFolders')?.asList()?.each {addAllFoldersToMap(it, foldersMap)}
    }

    void addAllFoldersToMap(Folder folder, Map<UUID, Folder> foldersMap) {
        foldersMap[folder.id] = folder
        folder.childFolders.each {addAllFoldersToMap(it, foldersMap)}
    }

    @Override
    String getContentType() {
        "application/mauro.${Folder.class.simpleName.toLowerCase()}+json"
    }
}
