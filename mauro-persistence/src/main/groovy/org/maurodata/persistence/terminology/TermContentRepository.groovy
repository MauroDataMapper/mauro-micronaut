package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.terminology.Term
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class TermContentRepository extends AdministeredItemContentRepository {

    @Inject
    TermCacheableRepository termCacheableRepository

    @Inject
    TermRelationshipCacheableRepository termRelationshipCacheableRepository

    @Override
    Term readWithContentById(UUID id) {
        Term term = termCacheableRepository.readById(id)
        term.sourceTermRelationships = termRelationshipCacheableRepository.readAllBySourceTerm(term)
        term.targetTermRelationships = termRelationshipCacheableRepository.readAllByTargetTerm(term)
        term
    }

    @Override
    Boolean handles(Class clazz) {
        return clazz.simpleName == 'Term'
    }
}
