package uk.ac.ox.softeng.mauro.persistence.terminology

import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

class TermRelationshipTypeContentRepository extends AdministeredItemContentRepository {

    @Inject
    TermRelationshipTypeCacheableRepository termRelationshipTypeCacheableRepository

    @Inject
    TermRelationshipCacheableRepository termRelationshipCacheableRepository


}
