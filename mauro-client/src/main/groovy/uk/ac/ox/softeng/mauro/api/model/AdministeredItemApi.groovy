package uk.ac.ox.softeng.mauro.api.model

import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body

@CompileStatic
interface AdministeredItemApi<I extends AdministeredItem, P extends AdministeredItem> {

    I show(UUID id)

    I create(UUID parentId, @Body @NonNull I item)

    I update(UUID id, @Body @NonNull I item)

    HttpStatus delete(@NonNull UUID id, @Body @Nullable I item)

    ListResponse<I> list(UUID parentId)

}
