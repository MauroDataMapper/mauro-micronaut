package org.maurodata.controller.jobs

import org.maurodata.api.Paths
import org.maurodata.api.jobs.JobsApi
import org.maurodata.audit.Audit
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class JobsController implements JobsApi {
    @Audit
    @Get(Paths.JOBS_LIST_PAGED)
    ListResponse<Map> index(@Nullable PaginationParams params){
        ListResponse.from([])
    }
}
