package uk.ac.ox.softeng.mauro.persistence.terminology

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

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
}
