package org.maurodata.web

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable

@Introspected
@CompileStatic
class PaginationParams {

    @Nullable
    Integer offset = 0

    @Nullable
    Integer max = -1

    @Nullable
    String sort = null

    @Nullable
    String order = "asc"

    @Nullable
    String label = null

    @Nullable
    String description = null

    @Nullable
    String code = null

    @Nullable
    String definition = null

    @Nullable
    String all = false

    @Nullable
    String domainType = null
}
