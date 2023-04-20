package uk.ac.ox.softeng.mauro.terminology

import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Inject
import reactor.core.publisher.Mono

@Controller('/terminologies/{terminologyId}/termRelationships')
class TermRelationshipController {

    final static List<String> DISALLOWED_PROPERTIES = ['class', 'id']

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    TermRepository termRepository

    @Inject
    TermRelationshipRepository termRelationshipRepository

    @Get('/{id}')
    Mono<TermRelationship> show(UUID terminologyId, UUID id) {
        termRelationshipRepository.findByTerminologyIdAndId(terminologyId, id)
    }

    @Post
    Mono<TermRelationship> create(UUID terminologyId, @Body TermRelationship termRelationship) {
        Mono.zip(terminologyRepository.findById(terminologyId), termRepository.findByTerminologyIdAndId(terminologyId, termRelationship.sourceTerm.id),
                 termRepository.findByTerminologyIdAndId(terminologyId, termRelationship.targetTerm.id)).flatMap {Tuple3 tuple ->
            def (Terminology terminology, Term sourceTerm, Term targetTerm) = tuple
            termRelationship.terminology = terminology
            termRelationshipRepository.save(termRelationship)
        }
    }

    @Put('/{id}')
    Mono<TermRelationship> update(UUID terminologyId, UUID id, @Body TermRelationship termRelationship) {
        termRelationshipRepository.findByTerminologyIdAndId(terminologyId, id).flatMap {TermRelationship existing ->
            existing.properties.each {
                if (!DISALLOWED_PROPERTIES.contains(it.key)) {
                    existing[it.key] = termRelationship[it.key]
                }
            }
            Mono.when(termRepository.existsByTerminologyIdAndId(terminologyId, existing.sourceTerm.id),
                      termRepository.existsByTerminologyIdAndId(terminologyId, existing.targetTerm.id)).flatMap {
                termRelationshipRepository.update(existing)
            }
        }
    }

}
