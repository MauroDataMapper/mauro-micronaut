package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface TermApi extends AdministeredItemApi<Term, Terminology> {

    @Get(Paths.TERM_ID)
    Term show(UUID terminologyId, UUID id)

    @Post(Paths.TERM_LIST)
    Term create(UUID terminologyId, @Body @NonNull Term term)

    @Put(Paths.TERM_ID)
    Term update(UUID terminologyId, UUID id, @Body @NonNull Term term)

    @Delete(Paths.TERM_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable Term term)

    @Get(Paths.TERM_LIST)
    ListResponse<Term> list(UUID terminologyId)

    @Get(Paths.TERM_LIST_PAGED)
    ListResponse<Term> list(UUID terminologyId, @Nullable PaginationParams params)

    @Get(Paths.TERM_TREE)
    List<Term> tree(UUID terminologyId, @Nullable UUID id)

    @Get(Paths.TERM_CODE_SETS)
    ListResponse<CodeSet> getCodeSetsForTerm(UUID terminologyId, UUID id)

    @Get(Paths.TERM_CODE_SETS_PAGED)
    ListResponse<CodeSet> getCodeSetsForTerm(UUID terminologyId, UUID id, @Nullable PaginationParams params)

    @Get(Paths.TERM_DOI)
    Map doi(UUID id)
}