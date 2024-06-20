package uk.ac.ox.softeng.mauro.test.plugin.importer.json

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.plugin.JsonPluginConstants
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileImportParameters
import uk.ac.ox.softeng.mauro.plugin.importer.FileParameter
import uk.ac.ox.softeng.mauro.test.domain.datamodel.DataModelSpec

@MicronautTest
class JsonDataModelImporterPluginSpec extends Specification  {

    static String NAMESPACE = JsonPluginConstants.NAMESPACE
    static String NAME = JsonPluginConstants.JSON_DATA_MODEL_NAME
    static String VERSION = JsonPluginConstants.VERSION

    @Inject
    ObjectMapper objectMapper

    @Inject
    MauroPluginService mauroPluginService


    def "test JSON data model import"() {

        when:
        DataModel testDataModel = DataModelSpec.testDataModel

        ExportModel exportModel = ExportModel.build {
            dataModel testDataModel
            exportMetadata {
                namespace NAMESPACE
                name NAME
                version VERSION
            }
        }

        DataModelImporterPlugin jsonImportPlugin = mauroPluginService.getPlugin(DataModelImporterPlugin, NAMESPACE, NAME, VERSION)
        FileImportParameters fileImportParameters = new FileImportParameters()
        FileParameter fileParameter = new FileParameter()
        fileParameter.fileName = "import.json"
        fileParameter.fileType = "application/json"
        fileParameter.fileContents = objectMapper.writeValueAsBytes(exportModel)
        fileImportParameters.importFile = fileParameter

        List<DataModel> importedModels = jsonImportPlugin.importModels(fileImportParameters)

        then:
        importedModels.size() == 1
        // TODO: A diff between source and imported models would be good here
        importedModels.first().label == testDataModel.label
        importedModels.first().dataClasses.size() == testDataModel.dataClasses.size()
        importedModels.first().dataTypes.size() == testDataModel.dataTypes.size()

    }




}
