package org.maurodata.controller.html

import groovy.util.logging.Slf4j
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.View
import jakarta.inject.Inject
import org.maurodata.controller.datamodel.DataClassController
import org.maurodata.controller.datamodel.DataModelController
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.model.PathRepository

@Slf4j
@Controller
//@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class DataModelHtmlController {

    @Inject
    DataModelController dataModelController

    @Inject
    DataClassController dataClassController

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    @Inject
    DataElementCacheableRepository dataElementCacheableRepository

    @Inject
    DataTypeCacheableRepository dataTypeCacheableRepository

    @Inject
    PathRepository pathRepository

    @Produces(MediaType.TEXT_HTML)
    @View('dataModel.html')
    @Get('/html/dataModel/{id}')
    DataModel show(UUID id) {
        DataModel dataModel = dataModelController.show(id)

        dataModel.dataClasses = dataClassRepository.readAllByDataModelAndParentDataClassIsNull(dataModel).sort {it.label}

        dataModel.dataClasses.each {DataClass dataClass ->
            dataClass.dataClasses = dataClassRepository.readAllByParentDataClass(dataClass)
        }

        dataModel
    }

    @Produces(MediaType.TEXT_HTML)
    @View('content-browser.html')
    @Get('/html/browser/dataClass/{id}')
    Map<String, Object> showDataClass(UUID id) {
        DataClass dataClass = dataClassController.show(id)

        dataClass.dataElements = dataElementCacheableRepository.findAllByDataClass(dataClass)

        dataClass.dataElements.each {
            it.dataType = dataTypeCacheableRepository.findById(it.dataType.id)
        }

        String rowCount = dataClass.metadata.find {it.namespace == 'org.maurodata.plugin.sql.database.sqlserver' && it.key == 'row_count'}.value

        pathRepository.readParentItems(dataClass)

        DataClass schemaClass = dataClass.parentDataClass

        List<DataClass> siblingTables = dataClassRepository.readAllByParentDataClass(schemaClass).sort {it.label}

        [
            label: dataClass.label,
            rowCount: rowCount,
            dataElements: dataClass.dataElements.collect {
                [
                    label: it.label,
                    description: it.description,
                    dataTypeLabel: it.metadata.find {it.namespace == 'org.maurodata.plugin.sql.database.sqlserver' && it.key == 'data_type'}?.value,
                    rowCount: it.metadata.find {it.namespace == 'uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research' && it.key == 'rowCount'}?.value,
                    distinctValuesCount: it.metadata.find {it.namespace == 'org.maurodata.plugin.sql.database.sqlserver' && it.key == 'distinct_values_count'}?.value,
                    nonNullValuesCount: it.metadata.find {it.namespace == 'uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research' && it.key == 'notNullValuesCount'}?.value
                ]
            },
            tables: siblingTables.collect {
                [
                    label: it.label,
                    active: it.label == dataClass.label
                ]
            }
        ]
    }
}
