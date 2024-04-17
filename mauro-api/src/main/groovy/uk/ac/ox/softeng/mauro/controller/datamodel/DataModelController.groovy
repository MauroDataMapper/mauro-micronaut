package uk.ac.ox.softeng.mauro.controller.datamodel

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.multipart.CompletedFileUpload
import io.micronaut.http.multipart.CompletedPart
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.folder.Folder
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
import uk.ac.ox.softeng.mauro.plugin.importer.DataModelImporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.FileParameter
import uk.ac.ox.softeng.mauro.plugin.importer.ImportParameters
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

import java.nio.charset.StandardCharsets
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
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/dataModels/import/{namespace}/{name}{/version}')
    ListResponse<DataModel> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {

        DataModelImporterPlugin mauroPlugin = MauroPluginService.getPlugin(DataModelImporterPlugin, namespace, name, version)

        if(!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "DataModel import plugin with namespace: ${namespace}, name: ${name} not found")
        }

        Map<String, Object> importMap = Flux.from(body).collectList().block().collectEntries { CompletedPart cp ->
            if(cp instanceof CompletedFileUpload) {
                return [cp.name, new FileParameter(cp.filename, cp.contentType.toString(), cp.bytes)]
            } else {
                return [cp.name, new String(cp.bytes, StandardCharsets.UTF_8)]
            }

        }

        ImportParameters importParameters = objectMapper.convertValue(importMap, mauroPlugin.importParametersClass())

        DataModel imported = mauroPlugin.importDomain(importParameters)

        Folder folder = folderRepository.readById(importParameters.folderId)
        imported.folder = folder
        log.info '** about to saveWithContentBatched... **'
        DataModel savedImported = modelContentRepository.saveWithContent(imported)
        log.info '** finished saveWithContentBatched **'
        ListResponse.from([show(savedImported.id)])

    }

}
