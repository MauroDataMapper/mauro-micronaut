package uk.ac.ox.softeng.mauro.persistence.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.persistence.model.ItemRepository

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class MetadataRepository implements ItemRepository<Metadata> {

    @Override
    Class getDomainClass() {
        Metadata
    }



    @Query(value = '''select distinct namespace, key from core.metadata order by namespace;''')
    abstract List<NamespaceDTO> getNamespaces()

    Map<String, Set<String>> getNamespaceKeys() {
        getNamespaces().groupBy {it.namespace }.collectEntries { key, values ->
            [key, values.collect {it.key} as Set]
        }
    }

}
