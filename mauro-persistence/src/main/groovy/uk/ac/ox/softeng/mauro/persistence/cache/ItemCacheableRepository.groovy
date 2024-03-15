package uk.ac.ox.softeng.mauro.persistence.cache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.cache.annotation.Cacheable
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository
import uk.ac.ox.softeng.mauro.persistence.security.CatalogueUserRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
abstract class ItemCacheableRepository<I extends Item> implements ItemRepository<I> {

    static final String FIND_BY_ID = 'find'
    static final String READ_BY_ID = 'read'

    ItemRepository<I> repository
    String domainType

    ItemCacheableRepository(ItemRepository<I> itemRepository) {
        this.repository = itemRepository
        this.domainType = repository.domainClass.simpleName
    }

    I findById(UUID id) {
        cachedLookupById(FIND_BY_ID, domainType, id)
    }

    I readById(UUID id) {
        cachedLookupById(READ_BY_ID, domainType, id)
    }

    I save(I item) {
        I saved = repository.save(item)
        invalidate(item)
        saved
    }

    List<I> saveAll(Iterable<I> items) {
        List<I> saved = repository.saveAll(items)
        items.each {invalidate(it)}
        saved
    }

    I update(I item) {
        I updated = repository.update(item)
        invalidate(item)
        updated
    }

    List<I> updateAll(Iterable<I> items) {
        List<I> updated = repository.updateAll(items)
        items.each { invalidate(it)}
        updated
    }

    Long delete(I item) {
        Long deleted = repository.delete(item)
        invalidate(item)
        deleted
    }

    Long deleteAll(Iterable<I> items) {
        Long deleted = repository.deleteAll(items)
        items.each {invalidate(it)}
        deleted
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
    I cachedLookupById(String lookup, String domainType, UUID id) {
        switch(lookup) {
            case FIND_BY_ID -> repository.findById(id)
            case READ_BY_ID -> repository.readById(id)
        }
    }

    void invalidate(I item) {
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

    Boolean handles(Class clazz) {
        repository.handles(clazz)
    }

    Boolean handles(String domainType) {
        repository.handles(domainType)
    }

    // Cacheable Item Repository definitions

    @Singleton
    @CompileStatic
    static class CatalogueUserCacheableRepository extends ItemCacheableRepository<CatalogueUser> {
        CatalogueUserCacheableRepository(CatalogueUserRepository catalogueUserRepository) {
            super(catalogueUserRepository)
        }
    }
}
