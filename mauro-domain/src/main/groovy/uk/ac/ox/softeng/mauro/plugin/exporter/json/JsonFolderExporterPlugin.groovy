package uk.ac.ox.softeng.mauro.plugin.exporter.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.JsonPluginConstants
import uk.ac.ox.softeng.mauro.plugin.exporter.FolderExporterPlugin

@Slf4j
@Singleton
class JsonFolderExporterPlugin implements FolderExporterPlugin {

    String version = JsonPluginConstants.VERSION

    String displayName = 'JSON Folder Exporter'

    Boolean canExportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    byte[] exportModel(Folder model) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.folders.add(model)
        objectMapper.writeValueAsBytes(exportModel)
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
        ExportModel exportModel = new ExportModel(this)
        exportModel.folders.addAll(folders)
        objectMapper.writeValueAsBytes(exportModel)

    }
}
