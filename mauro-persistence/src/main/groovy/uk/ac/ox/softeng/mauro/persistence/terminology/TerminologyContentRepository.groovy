package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

@CompileStatic
@Bean
class TerminologyContentRepository extends ModelContentRepository<Terminology> {

    @Inject
    TerminologyCacheableRepository terminologyRepository

    @Inject
    TermCacheableRepository termRepository

    @Inject
    TermRelationshipTypeCacheableRepository termRelationshipTypeRepository

    @Inject
    TermRelationshipCacheableRepository termRelationshipRepository

    Terminology findWithContentById(UUID id) {
        Terminology terminology = terminologyRepository.findById(id)
        terminology.terms = termRepository.findAllByParent(terminology)
        terminology.termRelationshipTypes = termRelationshipTypeRepository.findAllByParent(terminology)
        terminology.termRelationships = termRelationshipRepository.findAllByParent(terminology)

        terminology
    }
}
