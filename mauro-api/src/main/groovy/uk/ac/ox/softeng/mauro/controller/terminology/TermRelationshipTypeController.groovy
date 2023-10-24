package uk.ac.ox.softeng.mauro.controller.terminology

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
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

@CompileStatic
@Controller('/terminologies/{terminologyId}/termRelationshipTypes')
class TermRelationshipTypeController extends AdministeredItemController<TermRelationshipType, Terminology> {

    TermRelationshipTypeController(TermRelationshipTypeRepository termRelationshipTypeRepository, TerminologyRepository terminologyRepository, AdministeredItemContentRepository<TermRelationshipType> administeredItemContentRepository) {
        super(TermRelationshipType, termRelationshipTypeRepository, terminologyRepository, administeredItemContentRepository)
    }

    @Get('/{id}')
    Mono<TermRelationshipType> show(UUID terminologyId, UUID id) {
        super.show(terminologyId, id)
    }

    @Post
    Mono<TermRelationshipType> create(UUID terminologyId, @Body @NonNull TermRelationshipType termRelationshipType) {
        super.create(terminologyId, termRelationshipType)
    }

    @Put('/{id}')
    Mono<TermRelationshipType> update(UUID terminologyId, UUID id, @Body @NonNull TermRelationshipType termRelationshipType) {
        super.update(terminologyId, id, termRelationshipType)
    }

    @Get
    Mono<ListResponse<TermRelationshipType>> list(UUID terminologyId) {
        super.list(terminologyId)
    }

    @Delete('/{id}')
    Mono<HttpStatus> delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationshipType termRelationshipType) {
        super.delete(terminologyId, id, termRelationshipType)
    }
}
