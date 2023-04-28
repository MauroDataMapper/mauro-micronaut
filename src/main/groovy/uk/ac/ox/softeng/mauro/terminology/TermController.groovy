package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.core.annotation.Nullable
import io.micronaut.core.annotation.Order
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.json.tree.JsonObject
import jakarta.inject.Inject
import reactor.core.publisher.Mono

@Controller('/terminologies/{terminologyId}/terms')
class TermController {

    final static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    TermRepository termRepository

    @Get('/{id}')
    Mono<Term> show(UUID terminologyId, UUID id) {
        termRepository.findByTerminologyIdAndId(terminologyId, id)
    }

    @Post
    Mono<Term> create(UUID terminologyId, @Body Term term) {
        terminologyRepository.findById(terminologyId).flatMap {Terminology terminology ->
            term.terminology = terminology
            termRepository.save(term)
        }
    }

    @Put('/{id}')
    Mono<Term> update(UUID terminologyId, UUID id, @Body Term term, @Body JsonObject body) {
        termRepository.findByTerminologyIdAndId(terminologyId, id).flatMap {Term existing ->
            existing.properties.each {
                if (!DISALLOWED_PROPERTIES.contains(it.key) && body.get(it.key)) {
                    existing[it.key] = term[it.key]
                }
            }
            termRepository.update(existing)
        }
    }

    @Get
    Mono<List<Term>> list(UUID terminologyId) {
        terminologyRepository.findById(terminologyId).map {
            it.terms
        }
    }

    @Delete('/{id}')
    Mono<Long> delete(UUID id, @Nullable @Body Term term) {
        if (term?.version == null) {
            termRepository.deleteById(id)
        } else {
            term.id = id
            termRepository.delete(term)
        }
    }

    @Get('/tree{/id}')
    Mono<List<Term>> tree(UUID terminologyId, @Nullable UUID id) {
        termRepository.childTermsByParent(terminologyId, id).collectList()
    }
}
