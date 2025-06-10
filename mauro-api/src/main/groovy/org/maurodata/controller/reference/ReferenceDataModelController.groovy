package org.maurodata.controller.reference

import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.reference.ReferenceDataModelApi
import org.maurodata.audit.Audit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
class ReferenceDataModelController implements ReferenceDataModelApi {

    ReferenceDataModelController(){}

    @Get(Paths.REFERENCE_DATA_MODELS_LIST)
    List<Map> listAll()
    {
        return []
    }

    @Audit
    @Post(Paths.FOLDER_REFERENCE_DATA_MODELS)
    Map create(@NonNull UUID id, @Body @NonNull Map referenceDataModel)
    {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Reference data model is not implemented")
        return null
    }
}