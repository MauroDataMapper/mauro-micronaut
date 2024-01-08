package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.model.ModelItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
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
class CacheableRepository<I extends ModelItem> {

    private static final String FIND_LOOKUP = 'find'
    private static final String READ_LOOKUP = 'read'
    private static final String FIND_ALL_LOOKUP = 'findAll'
    private static final String READ_ALL_LOOKUP = 'readAll'

    ModelItemRepository<I> repository
    String domainType

    CacheableRepository(ModelItemRepository<I> itemRepository) {
        this.repository = itemRepository
        this.domainType = repository.domainClass.simpleName
    }

    Mono<I> findById(UUID id) {
        log.debug 'CacheableRepository::findById'
        cachedLookupById(FIND_LOOKUP, domainType, id)
    }

    Flux<I> findAllByParent(AdministeredItem parent) {
        cachedLookupByParent(FIND_ALL_LOOKUP, domainType, parent)
    }

    Mono<I> readById(UUID id) {
        log.debug 'CacheableRepository::readById'
        cachedLookupById(READ_LOOKUP, domainType, id)
    }

    Flux<I> readAllByParent(AdministeredItem parent) {
        cachedLookupByParent(READ_ALL_LOOKUP, domainType, parent)
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

    @Cacheable
    Flux<I> cachedLookupByParent(String lookup, String domainType, AdministeredItem parent) {
        switch(lookup) {
            case FIND_ALL_LOOKUP -> repository.findAllByParent(parent)
            case READ_ALL_LOOKUP -> repository.readAllByParent(parent)
        }
    }

    Mono<I> save(I item) {
        repository.save(item)
    }

    Mono<I> update(I item) {
        repository.update(item)
    }

    Boolean handles(Class clazz) {
        repository.handles(clazz)
    }

    @Bean
    @CompileStatic
    static class CacheableTermRepository extends CacheableRepository<Term> {
        CacheableTermRepository(TermRepository termRepository) {
            super(termRepository)
        }
    }

    @Introspected
    @CompileStatic
    static class StringCacheKeyGenerator implements CacheKeyGenerator {
        @Override
        Object generateKey(AnnotationMetadata annotationMetadata, Object... params) {
            params = params.collect {it instanceof Item ? it.id : it}
            return params.join('_')
        }
    }
}
