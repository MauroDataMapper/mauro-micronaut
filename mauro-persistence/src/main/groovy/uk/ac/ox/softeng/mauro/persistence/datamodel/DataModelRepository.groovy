package uk.ac.ox.softeng.mauro.persistence.datamodel

import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.persistence.datamodel.dto.DataModelDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository

import groovy.transform.CompileStatic
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataModelRepository implements ModelRepository<DataModel> {

    @Inject
    DataModelDTORepository dataModelDTORepository

    @Nullable
    DataModel findById(UUID id) {
        dataModelDTORepository.findById(id) as DataModel
    }

    @Override
    Class getDomainClass() {
        DataModel
    }

    @Override
    @Nullable
    abstract List<DataModel> findAllByFolderId(UUID folderId)

    @Query(value = '''
        select * from datamodel.data_model where
   exists(select 1 from core.metadata
       where metadata.multi_facet_aware_item_id = data_model.id and
             metadata.namespace = :namespace);
''',
        nativeQuery = true)
    abstract List<DataModel> getAllModelsByNamespace(String namespace)

}
