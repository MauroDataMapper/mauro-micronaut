package uk.ac.ox.softeng.mauro.persistence.explorer

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.GenericRepository
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class ExplorerRepository implements GenericRepository<DataElement, UUID> {

    @Query(value = '''
        select de.id
        from datamodel.data_element de
            join core.metadata md on (md.multi_facet_aware_item_domain_type = 'DataElement' and md.multi_facet_aware_item_id=de.id)
        and de.data_class_id in (:dataClassIds)
        and md.namespace = 'uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column'
        and (
            (md.key = 'foreign_key_schema' and md.value = :coreSchema) or (md.key = 'foreign_key_table' and md.value = :coreTable)
        )
        ''')
    abstract Set<UUID> getIdsOfChildForeignKeyDataElements(List<UUID> dataClassIds, String coreSchema, String coreTable)

}
