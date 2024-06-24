package uk.ac.ox.softeng.mauro.persistence.cache

import groovy.transform.CompileStatic
import io.micronaut.cache.interceptor.CacheKeyGenerator
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.annotation.Introspected
import uk.ac.ox.softeng.mauro.domain.model.Item

@Introspected
@CompileStatic
class StringCacheKeyGenerator implements CacheKeyGenerator {
    @Override
    String generateKey(AnnotationMetadata annotationMetadata, Object... params) {
        params = params.collect {it instanceof Item ? it.id : it}
        return params.join('_')
    }
}