package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class TermRelationshipTypeContentRepository extends AdministeredItemContentRepository {

    @Inject
    TermRelationshipTypeCacheableRepository termRelationshipTypeCacheableRepository

    @Inject
    TermRelationshipCacheableRepository termRelationshipCacheableRepository

    @Override
    TermRelationshipType readWithContentById(UUID id) {
        TermRelationshipType relationshipType = termRelationshipTypeCacheableRepository.readById(id)
        relationshipType.termRelationships = termRelationshipCacheableRepository.readAllByRelationshipType(relationshipType)
        relationshipType
    }
}
