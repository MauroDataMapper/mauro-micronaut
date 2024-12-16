package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/terminologies/{terminologyId}/terms')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface TermApi extends AdministeredItemApi<Term, Terminology> {

    @Get('/{id}')
    Term show(UUID terminologyId, UUID id)

    @Post
    Term create(UUID terminologyId, @Body @NonNull Term term)

    @Put('/{id}')
    Term update(UUID terminologyId, UUID id, @Body @NonNull Term term)

    @Delete('/{id}')
    HttpStatus delete(UUID terminologyId, UUID id, @Body @Nullable Term term)

    @Get
    ListResponse<Term> list(UUID terminologyId)

    @Get("/tree{/id}")
    List<Term> tree(UUID terminologyId, @Nullable UUID id)

    @Get('/{id}/codeSets')
    ListResponse<CodeSet> getCodeSetsForTerm(UUID id)
}