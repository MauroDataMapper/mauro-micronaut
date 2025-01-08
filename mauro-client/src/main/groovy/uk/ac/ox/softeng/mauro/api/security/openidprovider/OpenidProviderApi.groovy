package uk.ac.ox.softeng.mauro.api.security.openidprovider

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths

import io.micronaut.http.annotation.Get

@MauroApi
interface OpenidProviderApi {

    @Get(Paths.OPENID_PROVIDER_LIST)
    List<OpenidConnectProvider> list()
}
