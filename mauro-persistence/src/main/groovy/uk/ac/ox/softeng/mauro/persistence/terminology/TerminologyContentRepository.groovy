package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.dto.TermDTO

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import reactor.util.function.Tuple3

@CompileStatic
@Bean
class TerminologyContentRepository extends ModelContentRepository<Terminology> {

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    TermRepository termRepository

    @Inject
    TermRelationshipTypeRepository termRelationshipTypeRepository

    @Inject
    TermRelationshipRepository termRelationshipRepository

    Mono<Terminology> findWithAssociations(UUID id) {
        terminologyRepository.findById(id).flatMap {
            Mono.zip(termRepository.findAllByTerminology(it).collectList(), termRelationshipTypeRepository.findAllByTerminology(it).collectList(),
                     termRelationshipRepository.findAllByTerminology(it).collectList()).map {Tuple3<List<Term>, List<TermRelationshipType>, List<TermRelationship>> tuple3 ->
                it.terms = tuple3.getT1()
                it.termRelationshipTypes = tuple3.getT2()
                it.termRelationships = tuple3.getT3()

                it
            }
        } as Mono<Terminology>
    }
}
