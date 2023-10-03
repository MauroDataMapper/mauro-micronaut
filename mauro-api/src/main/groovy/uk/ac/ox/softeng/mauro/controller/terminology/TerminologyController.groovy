package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
import uk.ac.ox.softeng.mauro.domain.terminology.TerminologyService
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.controller.model.ModelController
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
class TerminologyController extends ModelController<Terminology> {

    TerminologyRepository terminologyRepository

    @Inject
    TerminologyService terminologyService

    TerminologyController(TerminologyRepository terminologyRepository, FolderRepository folderRepository) {
        super(Terminology, terminologyRepository, folderRepository)
        this.terminologyRepository = terminologyRepository
    }

    @Get('/terminologies/{id}')
    Mono<Terminology> show(UUID id) {
        super.show(id)
    }

    @Transactional
    @Post('/folders/{folderId}/terminologies')
    Mono<Terminology> create(UUID folderId, @Body @NonNull Terminology terminology) {
        super.create(folderId, terminology)
    }

    @Put('/terminologies/{id}')
    Mono<Terminology> update(UUID id, @Body @NonNull Terminology terminology) {
        super.update(id, terminology)
    }

    @Transactional
    @Delete('/terminologies/{id}')
    Mono<HttpStatus> delete(UUID id, @Body @Nullable Terminology terminology) {
        super.delete(id, terminology)
    }

    @Get('/folders/{folderId}/terminologies')
    Mono<ListResponse<Terminology>> list(UUID folderId) {
        super.list(folderId)
    }

    @Get('/terminologies')
    Mono<ListResponse<Terminology>> listAll() {
        super.listAll()
    }

    @Transactional
    @Put('/terminologies/{id}/finalise')
    Mono<Terminology> finalise(UUID id, @Body FinaliseData finaliseData) {
        terminologyRepository.findById(id).flatMap {Terminology terminology ->
            Terminology finalised = terminologyService.finaliseModel(terminology, finaliseData.version, finaliseData.versionChangeType, finaliseData.versionTag)
            terminologyRepository.update(finalised)
        }
    }
}
