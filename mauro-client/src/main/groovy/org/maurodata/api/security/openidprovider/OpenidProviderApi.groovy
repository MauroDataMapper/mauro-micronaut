package org.maurodata.api.security.openidprovider

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.http.annotation.Get

@MauroApi
interface OpenidProviderApi {

    @Get(Paths.OPENID_PROVIDER_LIST)
    List<OpenidConnectProvider> list()
}
