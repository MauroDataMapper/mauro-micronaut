package org.maurodata.persistence.cache

import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.facet.AnnotationRepository
import org.maurodata.persistence.facet.EditRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.facet.ReferenceFileRepository
import org.maurodata.persistence.facet.RuleRepository
import org.maurodata.persistence.facet.SummaryMetadataRepository
import org.maurodata.persistence.facet.VersionLinkRepository
import org.maurodata.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Inject
import jakarta.inject.Singleton

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
        cacheableRepositories.find {it.handles(domainType)}
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
    static class VersionLinkCacheableRepository extends FacetCacheableRepository<VersionLink> {
        VersionLinkCacheableRepository(VersionLinkRepository versionLinkRepository) {
            super(versionLinkRepository)
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
    static class EditCacheableRepository extends FacetCacheableRepository<Edit> {
        EditCacheableRepository(EditRepository editRepository) {
            super(editRepository)
        }
    }

    @Singleton
    @CompileStatic
    static class RuleCacheableRepository extends FacetCacheableRepository<Rule> {
        RuleCacheableRepository(RuleRepository ruleRepository) {
            super(ruleRepository)
        }
    }

    @Singleton
    @CompileStatic
    static class AnnotationCacheableRepository extends FacetCacheableRepository<Annotation> {
        AnnotationCacheableRepository(AnnotationRepository annotationRepository) {
            super(annotationRepository)
        }

        // TODO: Are these overrides necessary?
        Annotation findById(UUID id) {
            cachedLookupById(FIND_BY_ID, Annotation.class.simpleName, id)
        }

        Annotation readById(UUID id) {
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

    @Singleton
    @CompileStatic
    static class ReferenceFileCacheableRepository extends FacetCacheableRepository<ReferenceFile> {
        ReferenceFileCacheableRepository(ReferenceFileRepository referenceFileRepository) {
            super(referenceFileRepository)
        }

        Long deleteById(UUID id) {
            Long deleted = ((ReferenceFileRepository)repository).deleteById(id)
            super.invalidate(id)
            deleted
        }
    }
}
