package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.json.tree.JsonObject
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import reactor.util.function.Tuple3

import javax.transaction.Transactional

@Controller('/terminologies')
class TerminologyController {

    final static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    TermRepository termRepository

    @Inject
    TermRelationshipTypeRepository termRelationshipTypeRepository

    @Get('/{id}')
    Mono<Terminology> show(UUID id) {
        terminologyRepository.findById(id)
    }

    @Post
    Mono<Terminology> create(@Body Terminology terminology) {
        terminologyRepository.save(terminology)
    }

    @Put('/{id}')
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

    @Get
    Mono<List<Terminology>> list() {
        terminologyRepository.findAll().collectList()
    }

    @Transactional // this is necessary here, otherwise there are multiple transactions
    @Delete('/{id}')
    Mono<Long> delete(UUID id, @Nullable @Body Terminology terminology) {
        if (terminology?.version == null) {
            terminologyRepository.deleteById(id).zipWith(termRepository.deleteByTerminologyId(id), termRelationshipTypeRepository.deleteByTerminologyId(id), Tuple3::getT1)
        } else {
            terminology.id = id
            terminologyRepository.delete(terminology).zipWith(termRepository.deleteByTerminologyId(id), termRelationshipTypeRepository.deleteByTerminologyId(id),
                                                              Tuple3::getT1)
        }
    }
}
