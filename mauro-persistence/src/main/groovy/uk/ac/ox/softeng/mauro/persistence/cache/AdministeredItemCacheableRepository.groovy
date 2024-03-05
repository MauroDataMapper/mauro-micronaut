package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataElementRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.EnumerationValueRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
abstract class AdministeredItemCacheableRepository<I extends AdministeredItem> extends ItemCacheableRepository<I> implements AdministeredItemRepository<I> {

    static final String FIND_ALL_BY_PARENT = 'findAll'
    static final String READ_ALL_BY_PARENT = 'readAll'

    AdministeredItemRepository<I> repository

    AdministeredItemCacheableRepository(AdministeredItemRepository<I> itemRepository) {
        super(itemRepository)
        repository = itemRepository
    }

    List<I> findAllByParent(AdministeredItem parent) {
        cachedLookupByParent(FIND_ALL_BY_PARENT, domainType, parent)
    }

    List<I> readAllByParent(AdministeredItem parent) {
        cachedLookupByParent(READ_ALL_BY_PARENT, domainType, parent)
    }

    I update(I oldItem, I newItem) {
        I updated = repository.update(newItem)
        invalidate(oldItem, newItem)
        updated
    }

    @Cacheable
    List<I> cachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        switch (lookup) {
            case FIND_ALL_BY_PARENT -> repository.findAllByParent(parent)
            case READ_ALL_BY_PARENT -> repository.readAllByParent(parent)
        }
    }

    @CacheInvalidate
    void invalidateCachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        null
    }

    @Override
    void invalidate(I item) {
        super.invalidate(item)
        // Invalidate collections that could contain the new item
        AdministeredItem parent = item.parent
        invalidateCachedLookupByParent(FIND_ALL_BY_PARENT, domainType, parent)
        invalidateCachedLookupByParent(READ_ALL_BY_PARENT, domainType, parent)
    }

    void invalidate(I oldItem, I newItem) {
        if (oldItem.parent && oldItem.parent.id != newItem.parent?.id) {
            invalidate(oldItem)
        }
        invalidate(newItem)
    }

    // Cacheable Administered Item Repository definitions

    @CompileStatic
    @Singleton
    static class TermCacheableRepository extends AdministeredItemCacheableRepository<Term> {
        TermCacheableRepository(TermRepository termRepository) {
            super(termRepository)
        }

        // not cached
        List<Term> readChildTermsByParent(UUID terminologyId, @Nullable UUID id) {
            ((TermRepository) repository).readChildTermsByParent(terminologyId, id)
        }
    }

    @CompileStatic
    @Singleton
    static class TermRelationshipCacheableRepository extends AdministeredItemCacheableRepository<TermRelationship> {
        TermRelationshipCacheableRepository(TermRelationshipRepository termRelationshipRepository) {
            super(termRelationshipRepository)
        }

        // not cached

        List<TermRelationship> readAllBySourceTerm(Term sourceTerm) {
            ((TermRelationshipRepository) repository).readAllBySourceTerm(sourceTerm)
        }

        List<TermRelationship> readAllByTargetTerm(Term targetTerm) {
            ((TermRelationshipRepository) repository).readAllByTargetTerm(targetTerm)
        }

        List<TermRelationship> readAllByRelationshipType(TermRelationshipType relationshipType) {
            ((TermRelationshipRepository) repository).readAllByRelationshipType(relationshipType)
        }
    }

    @CompileStatic
    @Singleton
    static class TermRelationshipTypeCacheableRepository extends AdministeredItemCacheableRepository<TermRelationshipType> {
        TermRelationshipTypeCacheableRepository(TermRelationshipTypeRepository termRelationshipTypeRepository) {
            super(termRelationshipTypeRepository)
        }
    }

    @Bean
    @CompileStatic
    static class DataClassCacheableRepository extends AdministeredItemCacheableRepository<DataClass> {
        DataClassCacheableRepository(DataClassRepository dataClassRepository) {
            super(dataClassRepository)
        }

        // not cached
        List<DataClass> readAllByParentDataClass_Id(UUID parentDataClassId) {
            ((DataClassRepository) repository).readAllByParentDataClass_Id(parentDataClassId)
        }
    }

    @Bean
    @CompileStatic
    static class DataElementCacheableRepository extends AdministeredItemCacheableRepository<DataElement> {
        DataElementCacheableRepository(DataElementRepository dataElementRepository) {
            super(dataElementRepository)
        }
    }

    @Bean
    @CompileStatic
    static class DataTypeCacheableRepository extends AdministeredItemCacheableRepository<DataType> {
        DataTypeCacheableRepository(DataTypeRepository dataTypeRepository) {
            super(dataTypeRepository)
        }
    }

    @Bean
    @CompileStatic
    static class EnumerationValueCacheableRepository extends AdministeredItemCacheableRepository<EnumerationValue> {
        EnumerationValueCacheableRepository(EnumerationValueRepository enumerationValueRepository) {
            super(enumerationValueRepository)
        }

        // not cached
        List<EnumerationValue> readAllByEnumerationType_Id(UUID enumerationTypeId) {
            ((EnumerationValueRepository) repository).readAllByEnumerationType_Id(enumerationTypeId)
        }

    }

}