package uk.ac.ox.softeng.mauro.persistence.cache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class CacheableMetadataRepository implements ItemRepository<Metadata> {

    private static final String FIND_BY_ID = 'find'
    private static final String READ_BY_ID = 'read'
    private static final String FIND_ALL_BY_PARENT = 'findAll'
    private static final String READ_ALL_BY_PARENT = 'readAll'

    MetadataRepository repository
    String domainType

    @Inject
    List<CacheableAdministeredItemRepository> cacheableRepositories

    CacheableMetadataRepository(MetadataRepository metadataRepository) {
        this.repository = metadataRepository
        this.domainType = repository.domainClass.simpleName
    }

    Mono<Metadata> findById(UUID id) {
        log.debug 'CacheableMetadataRepository::findById'
        cachedLookupById(FIND_BY_ID, domainType, id)
    }

    Mono<Metadata> readById(UUID id) {
        log.debug 'CacheableMetadataRepository::readById'
        cachedLookupById(READ_BY_ID, domainType, id)
    }

    Mono<Metadata> save(Metadata item) {
        invalidateForItem(item).then(
                repository.save(item)
        )
    }

    Flux<Metadata> saveAll(Iterable<Metadata> items) {
        Flux.fromIterable(items).flatMap { invalidateForItem(it) }.thenMany(
                repository.saveAll(items)
        )
    }

    Mono<Metadata> update(Metadata item) {
        invalidateForItem(item).then(
                repository.update(item)
        )
    }

    Mono<Long> delete(Metadata item) {
        invalidateForItem(item).then(
                repository.delete(item)
        )
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
    Mono<Metadata> cachedLookupById(String lookup, String domainType, UUID id) {
        switch (lookup) {
            case FIND_BY_ID -> repository.findById(id)
            case READ_BY_ID -> repository.readById(id)
        }
    }

    @CacheInvalidate
    void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
        null
    }

    Mono<Void> invalidateForItem(Metadata item) {
        // invalidate the item
        invalidateCachedLookupById(FIND_BY_ID, domainType, item.id)
        invalidateCachedLookupById(READ_BY_ID, domainType, item.id)

        // invalidate find of the parent item
        invalidateCachedLookupById(FIND_BY_ID, item.multiFacetAwareItemDomainType, item.multiFacetAwareItemId)

        // invalidate findAll of the parent collection
        getRepository(item.multiFacetAwareItemDomainType).readById(item.multiFacetAwareItemId).flatMap { AdministeredItem parent ->
            invalidateCachedLookupById(FIND_ALL_BY_PARENT, item.multiFacetAwareItemDomainType, parent.parent.id)
        }
    }

    @NonNull
    CacheableAdministeredItemRepository getRepository(String domainType) {
        cacheableRepositories.find { it.domainType == domainType }
    }

    Class getDomainClass() {
        repository.domainClass
    }
}
