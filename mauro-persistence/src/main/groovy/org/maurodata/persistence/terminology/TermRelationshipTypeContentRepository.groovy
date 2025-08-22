package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

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

    @Override
    Boolean handles(Class clazz) {
        return clazz.simpleName == 'TermRelationshipType'
    }
}
