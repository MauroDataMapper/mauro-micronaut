package org.maurodata.api.inspect

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

@MauroApi
interface InspectApi {

    @Get(Paths.INSPECT_ITEM)
    Map<String, Object> inspect(String domainType, UUID id)

    @Get(Paths.INSPECT_ITEM_OVERVIEW)
    @Consumes('text/plain;charset=UTF-8')
    @Produces('text/plain;charset=UTF-8')
    String overview(String domainType, UUID id)

}
