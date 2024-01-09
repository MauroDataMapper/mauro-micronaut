package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.model.Item

import groovy.transform.CompileStatic
import io.micronaut.cache.interceptor.CacheKeyGenerator
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.Introspected

@Introspected
@CompileStatic
class StringCacheKeyGenerator implements CacheKeyGenerator {
    @Override
    Object generateKey(AnnotationMetadata annotationMetadata, Object... params) {
        params = params.collect {it instanceof Item ? it.id : it}
        return params.join('_')
    }
}