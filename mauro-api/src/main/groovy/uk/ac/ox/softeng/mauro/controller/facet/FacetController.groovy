package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService

@CompileStatic
abstract class FacetController<I extends Facet> extends ItemController<I> {

    ItemCacheableRepository<I> facetRepository

    @Inject
    RepositoryService repositoryService

    FacetController(ItemCacheableRepository<I> facetRepository) {
        super(facetRepository)
        this.facetRepository = facetRepository
    }

    @Get('/{id}')
    I show(UUID id) {
        facetRepository.findById(id)
    }

    @Post
    I create(String domainType, UUID domainId, @Body @NonNull I facet) {
        cleanBody(facet)

        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
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
        if (facet?.version) facetToDelete.version = facet.version
        Long deleted = facetRepository.delete(facetToDelete)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    protected AdministeredItem readAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        AdministeredItem administeredItem = administeredItemRepository.readById(domainId)
        if (!administeredItem) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        administeredItem
    }

    protected AdministeredItem findAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        AdministeredItem administeredItem = administeredItemRepository.findById(domainId)
        if (!administeredItem) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        administeredItem
    }

    protected AdministeredItemCacheableRepository getAdministeredItemRepository(String domainType) {
        AdministeredItemCacheableRepository administeredItemRepository = repositoryService.getAdministeredItemRepository(domainType)
        if (!administeredItemRepository) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Domain type [$domainType] not found")
        administeredItemRepository
    }

    protected I validate(String domainType, UUID domainId, UUID id) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        I facet = facetRepository.findById(id)
        if (facet) {
            if (facet.multiFacetAwareItemId != domainId || !administeredItemRepository.handles(domainType)){
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "DomainType or Domain Id not found for $id")
            }
        }
        facet
    }
}