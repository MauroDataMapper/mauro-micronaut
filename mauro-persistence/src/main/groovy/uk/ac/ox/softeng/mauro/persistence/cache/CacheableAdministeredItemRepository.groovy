package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.Nullable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class CacheableAdministeredItemRepository<I extends AdministeredItem> extends CacheableItemRepository<I> implements AdministeredItemRepository<I> {

    private static final String FIND_ALL_BY_PARENT = 'findAll'
    private static final String READ_ALL_BY_PARENT = 'readAll'

    AdministeredItemRepository<I> repository

    CacheableAdministeredItemRepository(AdministeredItemRepository<I> itemRepository) {
        super(itemRepository)
        repository = itemRepository
    }

    Flux<I> findAllByParent(AdministeredItem parent) {
        cachedLookupByParent(FIND_ALL_BY_PARENT, domainType, parent).flatMapIterable {it}
    }

    Flux<I> readAllByParent(AdministeredItem parent) {
        cachedLookupByParent(READ_ALL_BY_PARENT, domainType, parent).flatMapIterable {it}
    }

    Mono<I> update(AdministeredItem oldItem, AdministeredItem newItem) {
        invalidateOnUpdate(oldItem, newItem)
        repository.update(newItem)
    }

    @Cacheable
    Mono<List<I>> cachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        switch(lookup) {
            case FIND_ALL_BY_PARENT -> repository.findAllByParent(parent).collectList()
            case READ_ALL_BY_PARENT -> repository.readAllByParent(parent).collectList()
        }
    }

    @CacheInvalidate
    void invalidateCachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        null
    }

    @Override
    void invalidateOnSave(Item item) {
        super.invalidateOnSave(item)
        // Invalidate collections that could contain the new item
        AdministeredItem parent = ((AdministeredItem) item).parent
        invalidateCachedLookupByParent(FIND_ALL_BY_PARENT, domainType, parent)
        invalidateCachedLookupByParent(READ_ALL_BY_PARENT, domainType, parent)
    }

    @Override
    void invalidateOnUpdate(Item item) {
        super.invalidateOnUpdate(item)
        // Invalidate collections that could contain the updated item
        AdministeredItem parent = ((AdministeredItem) item).parent
        invalidateCachedLookupByParent(FIND_ALL_BY_PARENT, domainType, parent)
        invalidateCachedLookupByParent(READ_ALL_BY_PARENT, domainType, parent)
    }

    void invalidateOnUpdate(AdministeredItem oldItem, AdministeredItem newItem) {
        if (oldItem.parent && oldItem.parent.id != newItem.parent?.id) {
            invalidateOnUpdate(oldItem)
        }
        invalidateOnUpdate(newItem)
    }

    @Override
    void invalidateOnDelete(Item item) {
        super.invalidateOnDelete(item)
        // Invalidate collections that could contain the deleted item
        AdministeredItem parent = ((AdministeredItem) item).parent
        invalidateCachedLookupByParent(FIND_ALL_BY_PARENT, parent?.domainType, parent)
        invalidateCachedLookupByParent(READ_ALL_BY_PARENT, parent?.domainType, parent)
    }

    // Cacheable Administered Item Repository definitions

    @Bean
    @CompileStatic
    static class CacheableTermRepository extends CacheableAdministeredItemRepository<Term> {
        CacheableTermRepository(TermRepository termRepository) {
            super(termRepository)
        }

        // not cached
        Flux<Term> readChildTermsByParent(UUID terminologyId, @Nullable UUID id) {
            ((TermRepository) repository).readChildTermsByParent(terminologyId, id)
        }
    }

    @Bean
    @CompileStatic
    static class CacheableTermRelationshipRepository extends CacheableAdministeredItemRepository<TermRelationship> {
        CacheableTermRelationshipRepository(TermRelationshipRepository termRelationshipRepository) {
            super(termRelationshipRepository)
        }
    }

    @Bean
    @CompileStatic
    static class CacheableTermRelationshipTypeRepository extends CacheableAdministeredItemRepository<TermRelationshipType> {
        CacheableTermRelationshipTypeRepository(TermRelationshipTypeRepository termRelationshipTypeRepository) {
            super(termRelationshipTypeRepository)
        }
    }
}