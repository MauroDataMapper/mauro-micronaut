package uk.ac.ox.softeng.mauro.persistence.terminology

import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableModelRepository.CacheableTerminologyRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRelationshipRepository
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
    CacheableTerminologyRepository terminologyRepository

    @Inject
    CacheableTermRepository termRepository

    @Inject
    CacheableTermRelationshipTypeRepository termRelationshipTypeRepository

    @Inject
    CacheableTermRelationshipRepository termRelationshipRepository

    Mono<Terminology> findWithAssociations(UUID id) {
        terminologyRepository.findById(id).flatMap {
            Mono.zip(termRepository.findAllByParent(it).collectList(), termRelationshipTypeRepository.findAllByParent(it).collectList(),
                     termRelationshipRepository.findAllByParent(it).collectList()).map {Tuple3<List<Term>, List<TermRelationshipType>, List<TermRelationship>> tuple3 ->
                it.terms = tuple3.getT1()
                it.termRelationshipTypes = tuple3.getT2()
                it.termRelationships = tuple3.getT3()

                it
            }
        } as Mono<Terminology>
    }
}
