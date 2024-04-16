package uk.ac.ox.softeng.mauro.controller.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Consumes
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import io.micronaut.http.exceptions.HttpStatusException
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModelService
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.export.ExportMetadata
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

import java.time.Instant

@Slf4j
@Controller
@CompileStatic
class DataModelController extends ModelController<DataModel> {

    DataModelCacheableRepository dataModelRepository

    DataModelContentRepository dataModelContentRepository

    @Inject
    DataModelService dataModelService

    @Inject
    ObjectMapper objectMapper


    DataModelController(DataModelCacheableRepository dataModelRepository, FolderCacheableRepository folderRepository, DataModelContentRepository dataModelContentRepository) {
        super(DataModel, dataModelRepository, folderRepository, dataModelContentRepository)
        this.dataModelRepository = dataModelRepository
        this.dataModelContentRepository = dataModelContentRepository
    }

    @Get('/dataModels/{id}')
    DataModel show(UUID id) {
        super.show(id)
    }

    @Transactional
    @Post('/folders/{folderId}/dataModels')
    DataModel create(UUID folderId, @Body @NonNull DataModel dataModel) {
        super.create(folderId, dataModel)
    }

    @Put('/dataModels/{id}')
    DataModel update(UUID id, @Body @NonNull DataModel dataModel) {
        super.update(id, dataModel)
    }

    @Transactional
    @Delete('/dataModels/{id}')
    HttpStatus delete(UUID id, @Body @Nullable DataModel dataModel) {
        DataModel model = super.showNested(id) as DataModel
        if (!model){
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Model not found, $id")
        }
        super.delete(model, dataModel)
    }

    @Get('/folders/{folderId}/dataModels')
    ListResponse<DataModel> list(UUID folderId) {
        super.list(folderId)
    }

    @Get('/dataModels')
    ListResponse<DataModel> listAll() {
        super.listAll()
    }

    @Transactional
    @Put('/dataModels/{id}/finalise')
    DataModel finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Transactional
    @Put('/dataModels/{id}/newBranchModelVersion')
    DataModel createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Get('/dataModels/{id}/export{/namespace}{/name}{/version}')
    ExportModel exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        log.debug "*** exportModel start ${Instant.now()} ***"
        DataModel dataModel = dataModelContentRepository.findWithAssociations(id)
        log.debug "*** exportModel fetched ${Instant.now()} ***"
        dataModel.setAssociations()
        log.debug "*** setAssociations finished ${Instant.now()} ***"
        new ExportModel(
                exportMetadata: new ExportMetadata(
                        namespace: 'uk.ac.ox.softeng.mauro',
                        name: 'mauro-micronaut',
                        version: 'SNAPSHOT',
                        exportDate: Instant.now(),
                        exportedBy: 'USER@example.org'
                ),
                dataModel: dataModel
        )
    }

    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/dataModels/import/{namespace}/{name}{/version}')
    ListResponse<DataModel> importModel(@Body Map<String, String> importMap, String namespace, String name, @Nullable String version) {
        System.err.println(MauroPluginService.listPlugins())
        System.err.println(MauroPluginService.listImporterPlugins())
        System.err.println(MauroPluginService.listPlugins(ModelImporterPlugin))
        MauroPlugin mauroPlugin = MauroPluginService.getPlugin(namespace, name, version)
        System.err.println(mauroPlugin.displayName)
        new ListResponse()
    }

}
