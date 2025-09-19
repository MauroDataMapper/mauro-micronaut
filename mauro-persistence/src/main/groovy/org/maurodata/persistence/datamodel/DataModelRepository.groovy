package org.maurodata.persistence.datamodel

import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import org.maurodata.FieldConstants
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.persistence.datamodel.dto.DataModelDTORepository
import org.maurodata.persistence.model.ModelRepository

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

    @Nullable
    List<DataModel> findAllByParentAndPathIdentifier(UUID item, String pathIdentifier) {
        dataModelDTORepository.findAllByParentAndPathIdentifier(item, pathIdentifier)
    }

    @Nullable
    @Override
    List<DataModel> findAllByLabelContaining(String pathIdentifier) {
        dataModelDTORepository.findAllByLabelContaining(pathIdentifier)
    }
    @Override
    Class getDomainClass() {
        DataModel
    }

    @Override
    @Nullable
    abstract List<DataModel> findAllByFolderId(UUID folderId)

    // TODO: This method really needs caching
    @Query(value = '''
        select * from datamodel.data_model where
   exists(select 1 from core.metadata
       where metadata.multi_facet_aware_item_id = data_model.id and
             metadata.namespace = :namespace);
''',
        nativeQuery = true)
    abstract List<DataModel> getAllModelsByNamespace(String namespace)


    @Override
    Boolean handles(String domainType) {
        return domainType != null && domainType.toLowerCase() in [FieldConstants.DATAMODEL_LOWERCASE, FieldConstants.DATAMODELS_LOWERCASE]
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'dm'.equalsIgnoreCase(pathPrefix)
    }
}
