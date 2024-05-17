package uk.ac.ox.softeng.mauro.plugin.exporter.json

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.exporter.DataModelExporterPlugin

@Slf4j
@Singleton
class JsonDataModelExporterPlugin implements DataModelExporterPlugin {

    String version = '4.0.0'

    String displayName = 'JSON DataModel Exporter'

    Boolean canExportMultipleDomains = true

    @Inject
    ObjectMapper objectMapper

    @Override
    byte[] exportModel(DataModel dataModel) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.dataModel = dataModel
        objectMapper.writeValueAsBytes(exportModel)
    }

    @Override
    String getFileExtension() {
        return ".json"
    }

    @Override
    String getFileName(DataModel model) {
        return model.label + ".json"
    }

    @Override
    byte[] exportModels(Collection<DataModel> dataModels) {
        ExportModel exportModel = new ExportModel(this)
        exportModel.dataModels = dataModels
        objectMapper.writeValueAsBytes(exportModel)

    }
}
