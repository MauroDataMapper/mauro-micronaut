package uk.ac.ox.softeng.mauro.controller.terminology

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository

@Controller('/terminologies/{terminologyId}/termRelationshipTypes')
class TermRelationshipTypeController {

    final static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    TermRelationshipTypeRepository termRelationshipTypeRepository

    @Get('/{id}')
    Mono<TermRelationshipType> show(UUID terminologyId, UUID id) {
        termRelationshipTypeRepository.findByTerminologyIdAndId(terminologyId, id)
    }

    @Post
    Mono<TermRelationshipType> create(UUID terminologyId, @Body TermRelationshipType termRelationshipType) {
        terminologyRepository.findById(terminologyId).flatMap { Terminology terminology ->
            termRelationshipType.terminology = terminology
            termRelationshipTypeRepository.save(termRelationshipType)
        }
    }

    @Put('/{id}')
    Mono<TermRelationshipType> update(UUID terminologyId, UUID id, @Body TermRelationshipType termRelationshipType) {
        termRelationshipTypeRepository.findByTerminologyIdAndId(terminologyId, id).flatMap {TermRelationshipType existing ->
            existing.properties.each {
                if (!DISALLOWED_PROPERTIES.contains(it.key) && termRelationshipType[it.key] != null) {
                    existing[it.key] = termRelationshipType[it.key]
                }
            }
            termRelationshipTypeRepository.update(existing)
        }
    }

    @Get
    Mono<List<TermRelationshipType>> list(UUID terminologyId) {
        terminologyRepository.findById(terminologyId).map {
            it.termRelationshipTypes
        }
    }

    @Delete('/{id}')
    Mono<Long> delete(UUID id, @Nullable @Body TermRelationshipType termRelationshipType) {
        if (termRelationshipType?.version == null) {
            termRelationshipTypeRepository.deleteById(id)
        } else {
            termRelationshipType.id = id
            termRelationshipTypeRepository.delete(termRelationshipType)
        }
    }

}
