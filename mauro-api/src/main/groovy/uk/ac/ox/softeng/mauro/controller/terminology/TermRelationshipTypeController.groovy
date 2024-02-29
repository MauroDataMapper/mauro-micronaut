package uk.ac.ox.softeng.mauro.controller.terminology

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.TermRelationshipTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.TerminologyCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRelationshipTypeContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/terminologies/{terminologyId}/termRelationshipTypes')
class TermRelationshipTypeController extends AdministeredItemController<TermRelationshipType, Terminology> {

    TermRelationshipTypeController(TermRelationshipTypeCacheableRepository termRelationshipTypeRepository, TerminologyCacheableRepository terminologyRepository, TermRelationshipTypeContentRepository termRelationshipTypeContentRepository) {
        super(TermRelationshipType, termRelationshipTypeRepository, terminologyRepository, termRelationshipTypeContentRepository)
    }

    @Get('/{id}')
    TermRelationshipType show(UUID terminologyId, UUID id) {
        super.show(id)
    }

    @Post
    TermRelationshipType create(UUID terminologyId, @Body @NonNull TermRelationshipType termRelationshipType) {
        super.create(terminologyId, termRelationshipType)
    }

    @Put('/{id}')
    TermRelationshipType update(UUID terminologyId, UUID id, @Body @NonNull TermRelationshipType termRelationshipType) {
        super.update(id, termRelationshipType)
    }

    @Get
    ListResponse<TermRelationshipType> list(UUID terminologyId) {
        super.list(terminologyId)
    }

    @Delete('/{id}')
    HttpStatus delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationshipType termRelationshipType) {
        super.delete(id, termRelationshipType)
    }
}
