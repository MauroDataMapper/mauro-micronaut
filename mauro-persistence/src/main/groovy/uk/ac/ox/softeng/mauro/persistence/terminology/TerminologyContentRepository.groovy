package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRelationshipRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository.CacheableTermRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableModelRepository.CacheableTerminologyRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

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

    Terminology findWithAssociations(UUID id) {
        Terminology terminology = terminologyRepository.findById(id)
        terminology.terms = termRepository.findAllByParent(terminology)
        terminology.termRelationshipTypes = termRelationshipTypeRepository.findAllByParent(terminology)
        terminology.termRelationships = termRelationshipRepository.findAllByParent(terminology)

        terminology
    }
}
