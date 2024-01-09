package uk.ac.ox.softeng.mauro.controller.terminology


import uk.ac.ox.softeng.mauro.controller.model.CacheableAdministeredItemController
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableAdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.cache.CacheableItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import reactor.core.publisher.Mono
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

@CompileStatic
@Controller('/terminologies/{terminologyId}/terms')
class TermController extends CacheableAdministeredItemController<Term, Terminology> {

    CacheableAdministeredItemRepository.CacheableTermRepository termRepository

    TermController(CacheableAdministeredItemRepository.CacheableTermRepository termRepository, CacheableAdministeredItemRepository.CacheableTerminologyRepository terminologyRepository, AdministeredItemContentRepository<Term> administeredItemContentRepository) {
        super(Term, termRepository, terminologyRepository, administeredItemContentRepository)
        this.termRepository = termRepository
    }

    @Get('/{id}')
    Mono<Term> show(UUID terminologyId, UUID id) {
        super.show(terminologyId, id)
    }

    @Post
    Mono<Term> create(UUID terminologyId, @Body @NonNull Term term) {
        super.create(terminologyId, term)
    }

    @Put('/{id}')
    Mono<Term> update(UUID terminologyId, UUID id, @Body @NonNull Term term) {
        super.update(terminologyId, id, term)
    }

    @Delete('/{id}')
    Mono<HttpStatus> delete(UUID terminologyId, UUID id, @Body @Nullable Term term) {
        super.delete(terminologyId, id, term)
    }

    @Get
    Mono<ListResponse<Term>> list(UUID terminologyId) {
        super.list(terminologyId)
    }

//    @Get('/tree{/id}')
//    Mono<List<Term>> tree(UUID terminologyId, @Nullable UUID id) {
//        termRepository.readChildTermsByParent(terminologyId, id).collectList()
//    }
}
