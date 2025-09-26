package org.maurodata.api.path

import io.micronaut.http.annotation.Get
import org.maurodata.api.Paths
import org.maurodata.domain.model.Item

interface PathApi<I extends Item> {

    @Get(Paths.RESOURCE_BY_PATH)
    I getResourceByPath(String domainType, String path)

    @Get(Paths.RESOURCE_BY_PATH_FROM_RESOURCE)
    I getResourceByPathFromResource(String domainType, UUID domainId, String path)
}