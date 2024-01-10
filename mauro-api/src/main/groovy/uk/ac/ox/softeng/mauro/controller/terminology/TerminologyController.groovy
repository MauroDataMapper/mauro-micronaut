package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.domain.terminology.TerminologyService
import uk.ac.ox.softeng.mauro.export.ExportMetadata
import uk.ac.ox.softeng.mauro.export.ExportModel
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableModelRepository.CacheableTerminologyRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableModelRepository.CacheableFolderRepository
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.time.Instant

@Slf4j
@Controller
@CompileStatic
class TerminologyController extends ModelController<Terminology> {

    CacheableTerminologyRepository terminologyRepository

    TerminologyContentRepository terminologyContentRepository

    @Inject
    TerminologyService terminologyService

    @Inject
    ObjectMapper objectMapper

    TerminologyController(CacheableTerminologyRepository terminologyRepository, CacheableFolderRepository folderRepository, TerminologyContentRepository terminologyContentRepository) {
        super(Terminology, terminologyRepository, folderRepository, terminologyContentRepository)
        this.terminologyRepository = terminologyRepository
        this.terminologyContentRepository = terminologyContentRepository
    }

    @Get('/terminologies/{id}')
    Mono<Terminology> show(UUID id) {
        super.show(id)
    }

    @Transactional
    @Post('/folders/{folderId}/terminologies')
    Mono<Terminology> create(UUID folderId, @Body @NonNull Terminology terminology) {
        log.debug '*** TerminologyController.create ***'
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
        super.finalise(id, finaliseData)
    }

    @Transactional
    @Put('/terminologies/{id}/newBranchModelVersion')
    Mono<Terminology> createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Get('/terminologies/{id}/export{/namespace}{/name}{/version}')
    Mono<ExportModel> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        log.debug "*** exportModel start ${Instant.now()} ***"
        terminologyContentRepository.findWithAssociations(id).map {Terminology terminology ->
            log.debug "*** exportModel fetched ${Instant.now()} ***"
            terminology.setAssociations()
            log.debug "*** setAssociations finished ${Instant.now()} ***"
            new ExportModel(
                exportMetadata: new ExportMetadata(
                    namespace: 'uk.ac.ox.softeng.mauro',
                    name: 'mauro-micronaut',
                    version: 'SNAPSHOT',
                    exportDate: Instant.now(),
                    exportedBy: 'USER@example.org'
                ),
                terminology: terminology
            )
        }
    }

    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/terminologies/import{/namespace}{/name}{/version}')
    Mono<ListResponse<Terminology>> importModel(@Body Map<String, String> importMap, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        log.info '** start importModel **'
        ExportModel importModel = objectMapper.readValue(importMap.importFile, ExportModel)
        log.info '*** imported JSON model ***'
        Terminology imported = importModel.terminology
        imported.setAssociations()
        updateCreationProperties(imported)
        log.info '* start updateCreationProperties *'
        imported.getAllContents().each {updateCreationProperties(it)}
        log.info '* finish updateCreationProperties *'

        UUID folderId = UUID.fromString(importMap.folderId)

        folderRepository.readById(folderId).flatMap {Folder folder ->
            imported.folder = folder
            log.info '** about to saveWithContentBatched... **'
            modelContentRepository.saveWithAssociations(imported).flatMap {Terminology savedImported ->
                log.info '** finished saveWithContentBatched **'
                show(savedImported.id).map {ListResponse.from([it])}
            }
        }
    }
}
