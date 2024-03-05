package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.transaction.annotation.Transactional
import uk.ac.ox.softeng.mauro.controller.model.ItemController
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.MetadataCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.service.RepositoryService
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/{domainType}/{domainId}/metadata')
class MetadataController extends ItemController<Metadata> {

    MetadataCacheableRepository metadataRepository

    RepositoryService repositoryService

    MetadataController(MetadataCacheableRepository metadataRepository, RepositoryService repositoryService) {
        this.metadataRepository = metadataRepository
        this.repositoryService = repositoryService
    }

    @Get('/{id}')
    Metadata show(UUID id) {
        metadataRepository.findById(id)
    }

    @Post
    Metadata create(String domainType, UUID domainId, @Body @NonNull Metadata metadata) {
        cleanBody(metadata)

        AdministeredItem administeredItem = readAdministeredItem(domainType, domainId)
        createEntity(administeredItem, metadata)
    }

    protected Metadata createEntity(@NonNull AdministeredItem administeredItem, @NonNull Metadata cleanMetadata) {
        updateCreationProperties(cleanMetadata)

        cleanMetadata.multiFacetAwareItemDomainType = administeredItem.domainType
        cleanMetadata.multiFacetAwareItemId = administeredItem.id

        metadataRepository.save(cleanMetadata)
    }

    @Put('/{id}')
    Metadata update(UUID id, @Body @NonNull Metadata metadata) {
        cleanBody(metadata)

        Metadata existing = metadataRepository.readById(id)

        updateEntity(existing, metadata)
    }

    @Delete('/{id}')
    @Transactional
    HttpStatus delete(UUID id, @Body @Nullable Metadata metadata) {
        Metadata metadataToDelete = metadataRepository.readById(id)
        if (metadata?.version) metadataToDelete.version = metadata.version
        Long deleted = metadataRepository.delete(metadataToDelete)
        if (deleted) {
            HttpStatus.NO_CONTENT
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'Not found for deletion')
        }
    }

    @Get
    ListResponse<Metadata> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ListResponse.from(administeredItem.metadata)
    }

    protected Metadata updateEntity(@NonNull Metadata existing, @NonNull Metadata cleanMetadata) {
        boolean hasChanged = updateProperties(existing, cleanMetadata)

        if (hasChanged) {
            metadataRepository.update(existing)
        } else {
            existing
        }
    }

    AdministeredItem readAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = repositoryService.getAdministeredItemRepository(domainType)
        if (!administeredItemRepository) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Domain type [$domainType] not found")
        AdministeredItem administeredItem = administeredItemRepository.readById(domainId)
        if (!administeredItem) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        administeredItem
    }

    AdministeredItem findAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = repositoryService.getAdministeredItemRepository(domainType)
        if (!administeredItemRepository) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Domain type [$domainType] not found")
        AdministeredItem administeredItem = administeredItemRepository.findById(domainId)
        if (!administeredItem) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        administeredItem
    }
}
