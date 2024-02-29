package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

@CompileStatic
@Singleton
class TerminologyContentRepository extends ModelContentRepository<Terminology> {

    @Inject
    TerminologyCacheableRepository terminologyRepository

    @Inject
    TermCacheableRepository termRepository

    @Inject
    TermRelationshipTypeCacheableRepository termRelationshipTypeRepository

    @Inject
    TermRelationshipCacheableRepository termRelationshipRepository

    @Override
    Terminology readWithContentById(UUID id) {
        Terminology terminology = terminologyRepository.readById(id)
        terminology.terms = termRepository.readAllByParent(terminology)
        terminology.termRelationshipTypes = termRelationshipTypeRepository.readAllByParent(terminology)
        terminology.termRelationships = termRelationshipRepository.readAllByParent(terminology)

        terminology
    }

    @Override
    Terminology findWithContentById(UUID id) {
        Terminology terminology = terminologyRepository.findById(id)
        terminology.terms = termRepository.findAllByParent(terminology)
        terminology.termRelationshipTypes = termRelationshipTypeRepository.findAllByParent(terminology)
        terminology.termRelationships = termRelationshipRepository.findAllByParent(terminology)

        terminology
    }
}
