package uk.ac.ox.softeng.mauro.terminology

import uk.ac.ox.softeng.mauro.model.version.FinaliseData

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.json.tree.JsonObject
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import reactor.core.publisher.Mono

@Controller
class TerminologyController {

    final static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    TermRepository termRepository

    @Inject
    TermRelationshipTypeRepository termRelationshipTypeRepository

    @Inject
    TerminologyService terminologyService

    @Get('/terminologies/{id}')
    Mono<Terminology> show(UUID id) {
        terminologyRepository.findById(id)
    }

    @Post('/folders/{folderId}/terminologies')
    Mono<Terminology> create(UUID folderId, @Body Terminology terminology) {
        terminology.folderId = folderId
        terminology.createdBy = 'USER'
        terminologyRepository.save(terminology)
    }

    @Put('/terminologies/{id}')
    Mono<Terminology> update(UUID id, @Body Terminology terminology, @Body JsonObject body) {
        terminologyRepository.readById(id).flatMap {Terminology existing ->
            existing.properties.each {
                if (!DISALLOWED_PROPERTIES.contains(it.key) && body.get(it.key)) {
                    existing[it.key] = terminology[it.key]
                }
            }
            terminologyRepository.update(existing)
        }
    }

    @Get('/folders/{folderId}/terminologies')
    Mono<List<Terminology>> list(UUID folderId) {
        terminologyRepository.readAllByFolderId(folderId).collectList()
    }

    @Transactional // this is necessary here, otherwise there are multiple transactions
    @Delete('/terminologies/{id}')
    Mono<Long> delete(UUID id, @Nullable @Body Terminology terminology) {
        if (terminology?.version == null) {
            Mono.zip(terminologyRepository.deleteById(id), termRepository.deleteByTerminologyId(id), termRelationshipTypeRepository.deleteByTerminologyId(id)).map {
                it.getT1()
            }
        } else {
            terminology.id = id
            Mono.zip(terminologyRepository.delete(terminology), termRepository.deleteByTerminologyId(id), termRelationshipTypeRepository.deleteByTerminologyId(id)).map {
                it.getT1()
            }
        }
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
