package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.annotation.Bean
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.persistence.security.CatalogueUserRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class CacheableItemRepository<I extends Item> implements ItemRepository<I> {

    private static final String FIND_BY_ID = 'find'
    private static final String READ_BY_ID = 'read'

    ItemRepository<I> repository
    String domainType

    CacheableItemRepository(ItemRepository<I> itemRepository) {
        this.repository = itemRepository
        this.domainType = repository.domainClass.simpleName
    }

    Mono<I> findById(UUID id) {
        log.debug 'CacheableRepository::findById'
        cachedLookupById(FIND_BY_ID, domainType, id)
    }

    Mono<I> readById(UUID id) {
        log.debug 'CacheableRepository::readById'
        cachedLookupById(READ_BY_ID, domainType, id)
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
            case FIND_BY_ID -> repository.findById(id)
            case READ_BY_ID -> repository.readById(id)
        }
    }

    void invalidateOnSave(Item item) {

    }

    void invalidateOnUpdate(Item item) {
        invalidateCachedLookupById(FIND_BY_ID, domainType, item.id)
        invalidateCachedLookupById(READ_BY_ID, domainType, item.id)
    }

    void invalidateOnDelete(Item item) {
        invalidateCachedLookupById(FIND_BY_ID, domainType, item.id)
        invalidateCachedLookupById(READ_BY_ID, domainType, item.id)
    }

    @CacheInvalidate
    void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
        null
    }

    Class getDomainClass() {
        repository.domainClass
    }

    // Cacheable Item Repository definitions

    @Bean
    @CompileStatic
    static class CacheableMetadataRepository extends CacheableItemRepository<Metadata> {
        CacheableMetadataRepository(MetadataRepository metadataRepository) {
            super(metadataRepository)
        }
    }

    @Bean
    @CompileStatic
    static class CacheableCatalogueUserRepository extends CacheableItemRepository<CatalogueUser> {
        CacheableCatalogueUserRepository(CatalogueUserRepository catalogueUserRepository) {
            super(catalogueUserRepository)
        }
    }
}
