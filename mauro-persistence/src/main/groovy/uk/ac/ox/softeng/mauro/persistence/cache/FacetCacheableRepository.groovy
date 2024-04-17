package uk.ac.ox.softeng.mauro.persistence.cache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.facet.AnnotationRepository
import uk.ac.ox.softeng.mauro.persistence.facet.MetadataRepository
import uk.ac.ox.softeng.mauro.persistence.facet.SummaryMetadataRepository
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
abstract class FacetCacheableRepository<F extends Facet> extends ItemCacheableRepository<F> {

    static final String FIND_ALL_BY_PARENT = 'findAll'
    static final String READ_ALL_BY_PARENT = 'readAll'

    @Inject
    List<AdministeredItemCacheableRepository> cacheableRepositories

    FacetCacheableRepository(ItemRepository<F> itemRepository) {
        super(itemRepository)
    }

    // commented out @Override due to compilation issue when using <F extends Facet>
    // however <F extends Facet> instead of <I extends Facet> is needed otherwise @Inject doesn't work above
    //@Override
    void invalidate(F item) {
        // invalidate the metadata
        super.invalidate(item)

        // invalidate find of the parent item
        invalidateCachedLookupById(FIND_BY_ID, item.multiFacetAwareItemDomainType, item.multiFacetAwareItemId)

        // invalidate findAll of the parent collection
        AdministeredItem parent = getRepository(item.multiFacetAwareItemDomainType).readById(item.multiFacetAwareItemId)
        if (parent?.parent?.id) invalidateCachedLookupById(FIND_ALL_BY_PARENT, item.multiFacetAwareItemDomainType, parent.parent.id)
    }

    @NonNull
    AdministeredItemCacheableRepository<AdministeredItem> getRepository(String domainType) {
        cacheableRepositories.find {it.domainType == domainType}
    }

    // Cacheable Facet Repository definitions

    @Singleton
    @CompileStatic
    static class MetadataCacheableRepository extends FacetCacheableRepository<Metadata> {
        MetadataCacheableRepository(MetadataRepository metadataRepository) {
            super(metadataRepository)
        }
    }
    @Singleton
    @CompileStatic
    static class SummaryMetadataCacheableRepository extends FacetCacheableRepository<SummaryMetadata> {
        SummaryMetadataCacheableRepository(SummaryMetadataRepository summaryMetadataRepository) {
            super(summaryMetadataRepository)
        }
    }
    @Singleton
    @CompileStatic
    static class AnnotationCacheableRepository extends FacetCacheableRepository<Annotation> {
        AnnotationCacheableRepository(AnnotationRepository annotationRepository) {
            super(annotationRepository)
        }

        Annotation findById(UUID id) {
            cachedLookupById(FIND_BY_ID, Annotation.class.simpleName, id)
        }
        Annotation readById( UUID id) {
            cachedLookupById(READ_BY_ID, Annotation.class.simpleName, id)
        }

        List<Annotation> saveAll(Iterable<Annotation> items) {
            List<Annotation> savedChild = []
            List<Annotation> savedList = []
            Annotation saved
            items.forEach { annotation ->
                saved = repository.save(annotation)
                if (annotation.childAnnotations) {
                    annotation.childAnnotations.each { ch ->
                        ch.parentAnnotationId = saved.id
                    }
                    savedChild.addAll(repository.saveAll(annotation.childAnnotations))
                    savedChild.each {
                        invalidate(it)
                    }
                }
                savedList.add(saved)
                invalidate(saved)
            }
            savedList.addAll(savedChild)
            savedList
        }
    }
}
