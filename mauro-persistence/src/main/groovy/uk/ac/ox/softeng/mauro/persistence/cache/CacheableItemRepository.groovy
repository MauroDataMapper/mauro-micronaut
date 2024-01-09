package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.interceptor.CacheKeyGenerator
import io.micronaut.context.annotation.Bean
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.Introspected
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class CacheableItemRepository<I extends Item> implements ItemRepository<I> {

    private static final String FIND_LOOKUP = 'find'
    private static final String READ_LOOKUP = 'read'

    ItemRepository<I> repository
    String domainType

    CacheableItemRepository(ItemRepository<I> itemRepository) {
        this.repository = itemRepository
        this.domainType = repository.domainClass.simpleName
    }

    Mono<I> findById(UUID id) {
        log.debug 'CacheableRepository::findById'
        cachedLookupById(FIND_LOOKUP, domainType, id)
    }

    Mono<I> readById(UUID id) {
        log.debug 'CacheableRepository::readById'
        cachedLookupById(READ_LOOKUP, domainType, id)
    }

    /**
     * Single method to perform all cached repository methods.
     * This allows all responses to be cached in a single cache which can have a configured maximum size.
     *
     * @param lookup Type of repository lookup to perform
     * @param id Object UUID
     * @return Mono of the object
     */
    @Cacheable
    Mono<I> cachedLookupById(String lookup, String domainType, UUID id) {
        switch(lookup) {
            case FIND_LOOKUP -> repository.findById(id)
            case READ_LOOKUP -> repository.readById(id)
        }
    }

    void invalidateOnSave(Item item) {

    }

    void invalidateOnUpdate(Item item) {
        invalidateCachedLookupById(FIND_LOOKUP, domainType, item.id)
        invalidateCachedLookupById(READ_LOOKUP, domainType, item.id)
    }

    void invalidateOnDelete(Item item) {
        invalidateCachedLookupById(FIND_LOOKUP, domainType, item.id)
        invalidateCachedLookupById(READ_LOOKUP, domainType, item.id)
    }

    @CacheInvalidate
    void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
        null
    }

    Mono<I> save(I item) {
        invalidateOnSave(item)
        repository.save(item)
    }

    Flux<I> saveAll(Iterable<I> items) {
        items.each {invalidateOnSave(it)}
        repository.saveAll(items)
    }

    Mono<I> update(I item) {
        invalidateOnUpdate(item)
        repository.update(item)
    }

    Mono<Long> delete(I item) {
        repository.delete(item)
    }

    Class getDomainClass() {
        repository.domainClass
    }

    // Cacheable Item Repository definitions

}
