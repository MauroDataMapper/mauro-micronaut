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
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModelService
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.export.ExportMetadata
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import java.time.Instant

@Slf4j
@Controller
@CompileStatic
class DataModelController extends ModelController<DataModel> {

    DataModelCacheableRepository dataModelRepository

    DataModelContentRepository dataModelContentRepository

    @Inject
    DataModelService dataModelService


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
        super.importModel(body, namespace, name, version)
    }

    @Get('/dataModels/{id}/diff/{otherId}')
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        DataModel dataModel = super.showNested(id) as DataModel
        handleNotFoundError(dataModel, id)
        DataModel otherDataModel = super.showNested(otherId) as DataModel
        handleNotFoundError(otherDataModel, otherId)
        ObjectDiff diff = dataModel.diff(otherDataModel)
        diff
    }

    private void handleNotFoundError(DataModel dataModel, UUID id) {
        if (!dataModel) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Model not found, $id")
        }
    }
}
