package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.facet.FacetApi

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository

@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
abstract class FacetController<I extends Facet> extends ItemController<I> implements FacetApi<I> {

    ItemCacheableRepository<I> facetRepository

    FacetController(ItemCacheableRepository<I> facetRepository) {
        super(facetRepository)
        this.facetRepository = facetRepository
    }

    @Get('/{id}')
    I show(UUID id) {
        I facet = facetRepository.findById(id)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(facet))
        facet
    }

    @Post
    I create(String domainType, UUID domainId, @Body @NonNull I facet) {
        cleanBody(facet)

        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.EDITOR, administeredItem)

        createEntity(administeredItem, facet)
    }

    protected I createEntity(@NonNull AdministeredItem administeredItem, @NonNull I cleanFacet) {
        updateCreationProperties(cleanFacet)

        cleanFacet.multiFacetAwareItemDomainType = administeredItem.domainType
        cleanFacet.multiFacetAwareItemId = administeredItem.id

        facetRepository.save(cleanFacet)
    }

    @Put('/{id}')
    I update(UUID id, @Body @NonNull I facet) {
        cleanBody(facet)

        I existing = facetRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(existing))

        updateEntity(existing, facet)
    }

    protected I updateEntity(@NonNull I existing, @NonNull I cleanFacet) {
        boolean hasChanged = updateProperties(existing, cleanFacet)

        if (hasChanged) {
            facetRepository.update(existing)
        } else {
            existing
        }
    }

    @Delete('/{id}')
    @Transactional
    HttpStatus delete(UUID id, @Body @Nullable I facet) {
        I facetToDelete = facetRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(facetToDelete))
        if (facet?.version) facetToDelete.version = facet.version
        Long deleted = facetRepository.delete(facetToDelete)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    protected AdministeredItem readAdministeredItemForFacet(I facet) {
        if (facet) readAdministeredItem(facet.multiFacetAwareItemDomainType, facet.multiFacetAwareItemId)
    }

    protected I validateAndGet(String domainType, UUID domainId, UUID id) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        I facet = facetRepository.findById(id)
        if (facet) {
            if (facet.multiFacetAwareItemId != domainId || !administeredItemRepository.handles(domainType)) {
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "DomainType or Domain Id not found for $id")
            }
        }
        facet
    }
}