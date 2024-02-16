package uk.ac.ox.softeng.mauro.persistence.cache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.cache.annotation.CacheInvalidate
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class CacheableMetadataRepository extends CacheableItemRepository<Metadata> {

    static final String FIND_ALL_BY_PARENT = 'findAll'
    static final String READ_ALL_BY_PARENT = 'readAll'

    @Inject
    List<CacheableAdministeredItemRepository> cacheableRepositories

    CacheableMetadataRepository(MetadataRepository metadataRepository) {
        super(metadataRepository)
    }

    @CacheInvalidate
    void invalidateCachedLookupById(String lookup, String domainType, UUID id) {
        null
    }

    @Override
    void invalidate(Metadata item) {
        // invalidate the metadata
        super.invalidate(item)

        // invalidate find of the parent item
        invalidateCachedLookupById(FIND_BY_ID, item.multiFacetAwareItemDomainType, item.multiFacetAwareItemId)

        // invalidate findAll of the parent collection
        AdministeredItem parent = getRepository(item.multiFacetAwareItemDomainType).readById(item.multiFacetAwareItemId)
        invalidateCachedLookupById(FIND_ALL_BY_PARENT, item.multiFacetAwareItemDomainType, parent.parent.id)
    }

    @NonNull
    CacheableAdministeredItemRepository<AdministeredItem> getRepository(String domainType) {
        cacheableRepositories.find {it.domainType == domainType}
    }

    Class getDomainClass() {
        repository.domainClass
    }
}
