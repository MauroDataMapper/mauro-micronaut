package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.annotation.Bean
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class CacheableAdministeredItemRepository<I extends AdministeredItem> extends CacheableItemRepository<I> implements AdministeredItemRepository<I> {

    private static final String FIND_ALL_LOOKUP = 'findAll'
    private static final String READ_ALL_LOOKUP = 'readAll'

    AdministeredItemRepository<I> repository

    CacheableAdministeredItemRepository(AdministeredItemRepository<I> itemRepository) {
        super(itemRepository)
        repository = itemRepository
    }

    Flux<I> findAllByParent(AdministeredItem parent) {
        cachedLookupByParent(FIND_ALL_LOOKUP, domainType, parent).flatMapIterable {it}
    }

    Flux<I> readAllByParent(AdministeredItem parent) {
        cachedLookupByParent(READ_ALL_LOOKUP, domainType, parent).flatMapIterable {it}
    }

    @Cacheable
    Mono<List<I>> cachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        switch(lookup) {
            case FIND_ALL_LOOKUP -> repository.findAllByParent(parent).collectList()
            case READ_ALL_LOOKUP -> repository.readAllByParent(parent).collectList()
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
        invalidateCachedLookupByParent(FIND_ALL_LOOKUP, parent?.domainType, parent)
        invalidateCachedLookupByParent(READ_ALL_LOOKUP, parent?.domainType, parent)
    }

    void invalidateOnUpdate(Item item) {
        super.invalidateOnUpdate(item)

    }

    @Override


    // Cacheable Administered Item Repository definitions

    @Bean
    @CompileStatic
    static class CacheableTermRepository extends CacheableAdministeredItemRepository<Term> {
        CacheableTermRepository(TermRepository termRepository) {
            super(termRepository)
        }
    }

    @Bean
    @CompileStatic
    static class CacheableTerminologyRepository extends CacheableAdministeredItemRepository<Terminology> {
        CacheableTerminologyRepository(TerminologyRepository terminologyRepository) {
            super(terminologyRepository)
        }
    }
}
