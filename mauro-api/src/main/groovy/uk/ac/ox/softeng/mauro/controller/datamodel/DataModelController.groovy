package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModelService
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
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
import reactor.core.publisher.Mono

@Controller
@CompileStatic
class DataModelController extends ModelController<DataModel> {

    DataModelRepository dataModelRepository

    @Inject
    DataModelService dataModelService

    DataModelController(DataModelRepository dataModelRepository, FolderRepository folderRepository, ModelContentRepository<DataModel> modelContentRepository) {
        super(DataModel, dataModelRepository, folderRepository, modelContentRepository)
        this.dataModelRepository = dataModelRepository
    }

    @Get('/dataModels/{id}')
    Mono<DataModel> show(UUID id) {
        super.show(id)
    }

    @Transactional
    @Post('/folders/{folderId}/dataModels')
    Mono<DataModel> create(UUID folderId, @Body @NonNull DataModel dataModel) {
        super.create(folderId, dataModel)
    }

    @Put('/dataModels/{id}')
    Mono<DataModel> update(UUID id, @Body @NonNull DataModel dataModel) {
        super.update(id, dataModel)
    }

    @Transactional
    @Delete('/dataModels/{id}')
    Mono<HttpStatus> delete(UUID id, @Body @Nullable DataModel dataModel) {
        super.delete(id, dataModel)
    }

    @Get('/folders/{folderId}/dataModels')
    Mono<ListResponse<DataModel>> list(UUID folderId) {
        super.list(folderId)
    }

    @Get('/dataModels')
    Mono<ListResponse<DataModel>> listAll() {
        super.listAll()
    }

    @Transactional
    @Put('/dataModels/{id}/finalise')
    Mono<DataModel> finalise(UUID id, @Body FinaliseData finaliseData) {
        dataModelRepository.findById(id).flatMap {DataModel dataModel ->
            DataModel finalised = dataModelService.finaliseModel(dataModel, finaliseData.version, finaliseData.versionChangeType, finaliseData.versionTag)
            dataModelRepository.update(finalised)
        }
    }
}
