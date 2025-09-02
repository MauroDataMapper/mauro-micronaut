package org.maurodata.persistence.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import org.maurodata.persistence.model.ModelContentRepository

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

    @Override
    Terminology saveWithContent(@NonNull Terminology terminology) {
        (Terminology) super.saveWithContent(terminology)
    }

    @Override
    Boolean handles(String domainType) {
        return terminologyRepository.handles(domainType)
    }

    @Override
    Boolean handles(Class clazz) {
        return terminologyRepository.handles(clazz)
    }
}
