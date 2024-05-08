package uk.ac.ox.softeng.mauro.controller.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.transaction.annotation.Transactional
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModelService
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@Controller
@CompileStatic
class DataModelController extends ModelController<DataModel> {

    DataModelCacheableRepository dataModelRepository

    DataModelContentRepository dataModelContentRepository

    DataModelService dataModelService

    DataModelController(DataModelCacheableRepository dataModelRepository, FolderCacheableRepository folderRepository, DataModelContentRepository dataModelContentRepository,
    DataModelService dataModelService) {
        super(DataModel, dataModelRepository, folderRepository, dataModelContentRepository, dataModelService)
        this.dataModelRepository = dataModelRepository
        this.dataModelContentRepository = dataModelContentRepository
        this.dataModelService = dataModelService
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
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModel(id, namespace, name, version)
    }

    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/dataModels/import/{namespace}/{name}{/version}')
    ListResponse<DataModel> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }

}
