package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
import uk.ac.ox.softeng.mauro.domain.terminology.TerminologyService
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.controller.model.ModelController

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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Controller
@CompileStatic
class TerminologyController extends ModelController<Terminology> {

    TerminologyRepository terminologyRepository

    @Inject
    TerminologyService terminologyService

    TerminologyController(TerminologyRepository terminologyRepository, FolderRepository folderRepository, ModelContentRepository<Terminology> modelContentRepository) {
        super(Terminology, terminologyRepository, folderRepository, modelContentRepository)
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

    @Transactional
    @Put('/terminologies/{id}/newBranchModelVersion')
    Mono<Terminology> newBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        if (!createNewVersionData) createNewVersionData = new CreateNewVersionData()
        terminologyRepository.findById(id).flatMap {Terminology existing ->
            Terminology copy = terminologyService.createNewBranchModelVersion(existing, createNewVersionData.branchName)

            createEntity(copy.folder, copy).flatMap {Terminology savedCopy ->
                Flux.fromIterable(savedCopy.allContents).concatMap {AdministeredItem item ->
                    updateCreationProperties(item)

                }

                Mono.just(savedCopy)
            }
        }
    }
}
