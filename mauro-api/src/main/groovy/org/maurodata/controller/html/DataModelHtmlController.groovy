package org.maurodata.controller.html

import org.maurodata.controller.datamodel.DataModelController
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.View
import jakarta.inject.Inject

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class DataModelHtmlController {

    @Inject
    DataModelController dataModelController

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

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
}
