package uk.ac.ox.softeng.mauro.api.authority

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.authority.Authority
import uk.ac.ox.softeng.mauro.web.ListResponse

@MauroApi
interface AuthorityApi {

    @Get(Paths.AUTHORITY_ID)
    Authority show(@NonNull UUID id)

    @Get(Paths.AUTHORITY_LIST)
    ListResponse<Authority> list()

    @Post(Paths.AUTHORITY_LIST)
    Authority create(@Body @NonNull Authority authority)

    @Put(Paths.AUTHORITY_ID)
    Authority update(UUID id, @Body @NonNull Authority authority)

    @Delete(Paths.AUTHORITY_ID)
    HttpResponse delete(UUID id, @Body @Nullable Authority authority)
}
