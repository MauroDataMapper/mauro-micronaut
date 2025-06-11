package org.maurodata.api.model

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body

@CompileStatic
interface AdministeredItemApi<I extends AdministeredItem, P extends AdministeredItem> {

    I show(UUID id)

    I create(UUID parentId, @Body @NonNull I item)

    I update(UUID id, @Body @NonNull I item)

    HttpResponse delete(@NonNull UUID id, @Body @Nullable I item)

    ListResponse<I> list(UUID parentId)

}
