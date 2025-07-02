package org.maurodata.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.facet.Annotation
import org.maurodata.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class AnnotationRepository implements ItemRepository<Annotation> {

    @Query(''' select * from core.annotation a where a.parent_annotation_id = :id ''')
    @Nullable
    abstract Set<Annotation> findAllChildrenById(@NonNull UUID id)


    @Override
    Class getDomainClass() {
        Annotation
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'ann'.equalsIgnoreCase(pathPrefix)
    }
}
