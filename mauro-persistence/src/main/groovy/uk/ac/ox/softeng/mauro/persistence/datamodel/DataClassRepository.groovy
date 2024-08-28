package uk.ac.ox.softeng.mauro.persistence.datamodel

import io.micronaut.core.annotation.NonNull
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.datamodel.dto.DataClassDTORepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelItemRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import jakarta.inject.Inject

@Slf4j
@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class DataClassRepository implements ModelItemRepository<DataClass> {

    @Inject
    DataClassDTORepository dataClassDTORepository

    @Override
    @Nullable
    DataClass findById(UUID id) {
        log.debug 'DataClassRepository::findById'
        dataClassDTORepository.findById(id) as DataClass
    }

    @Nullable
    List<DataClass> findAllByDataModel(DataModel dataModel) {
        dataClassDTORepository.findAllByDataModel(dataModel) as List<DataClass>
    }

    @Override
    @Nullable
    List<DataClass> findAllByParent(AdministeredItem parent) {
        findAllByDataModel((DataModel) parent)
    }

    @Nullable
    abstract List<DataClass> readAllByDataModel(DataModel dataModel)

    @Nullable
    abstract List<DataClass> readAllByDataModel_Id(UUID dataModelId)


    @Nullable
    abstract List<DataClass> readAllByParentDataClass_Id(UUID dataClassId)

    @Override
    @Nullable
    List<DataClass> readAllByParent(AdministeredItem parent) {
        readAllByDataModel((DataModel) parent)
    }

    abstract Long deleteByDataModelId(UUID dataModelId)

    //    @Override
    Long deleteByOwnerId(UUID ownerId) {
        deleteByDataModelId(ownerId)
    }

    @Override
    @Nullable
    DataClass findWithContentById(@NonNull UUID id, @NonNull AdministeredItem parent){
        DataClass dataClass = findById(id)
        dataClass.parentDataClass = parent as DataClass
        dataClass
    }

    @Override
    Class getDomainClass() {
        DataClass
    }

    @Override
    Boolean handles(Class clazz) {
        domainClass.isAssignableFrom(clazz)
    }

    @Override
    Boolean handles(String domainType) {
        domainClass.simpleName.equalsIgnoreCase(domainType) || (domainClass.simpleName + 'es').equalsIgnoreCase(domainType)
    }

}

